package Chess;

import static Chess.BoardUtil.*;
import static Chess.Constants.DirectionConstants.*;
import static Chess.Constants.DirectionConstants.Direction.*;
import static Chess.Constants.PieceConstants.*;
import static Chess.Constants.PositionConstants.*;
import static Chess.Constants.PieceConstants.PieceColor.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.IntConsumer;

import Chess.Constants.DirectionConstants.Direction;
import Chess.Constants.PieceConstants.PieceColor;
import Chess.Constants.PieceConstants.PieceType;

public class BitboardHelper {
    public static final long RANK1 = 0x00000000000000FFL;
    public static final long RANK2 = 0x000000000000FF00L;
    public static final long RANK3 = 0x0000000000FF0000L;
    public static final long RANK4 = 0x00000000FF000000L;
    public static final long RANK5 = 0x000000FF00000000L;
    public static final long RANK6 = 0x0000FF0000000000L;
    public static final long RANK7 = 0x00FF000000000000L;
    public static final long RANK8 = 0xFF00000000000000L;

    public static final long RANKS[] = {
        RANK1, RANK2, RANK3, RANK4, RANK5, RANK6, RANK7, RANK8
    };

    public static final long FILE_A = 0x0101010101010101L;
    public static final long FILE_B = 0x0202020202020202L;
    public static final long FILE_C = 0x0404040404040404L;
    public static final long FILE_D = 0x0808080808080808L;
    public static final long FILE_E = 0x1010101010101010L;
    public static final long FILE_F = 0x2020202020202020L;
    public static final long FILE_G = 0x4040404040404040L;
    public static final long FILE_H = 0x8080808080808080L;

    public static final long[] FILES = {
        FILE_A, FILE_B, FILE_C, FILE_D, FILE_E, FILE_F, FILE_G, FILE_H
    };

    public static final long DOWN_DIAGONAL_1 = 0x0000000000000080L;
    public static final long DOWN_DIAGONAL_2 = 0x0000000000008040L;
    public static final long DOWN_DIAGONAL_3 = 0x0000000000804020L;
    public static final long DOWN_DIAGONAL_4 = 0x0000000080402010L;
    public static final long DOWN_DIAGONAL_5 = 0x0000008040201008L;
    public static final long DOWN_DIAGONAL_6 = 0x0000804020100804L;
    public static final long DOWN_DIAGONAL_7 = 0x0080402010080402L;
    public static final long DOWN_DIAGONAL_8 = 0x8040201008040201L;
    public static final long DOWN_DIAGONAL_9 = 0x4020100804020100L;
    public static final long DOWN_DIAGONAL_10 = 0x2010080402010000L;
    public static final long DOWN_DIAGONAL_11 = 0x1008040201000000L;
    public static final long DOWN_DIAGONAL_12 = 0x0804020100000000L;
    public static final long DOWN_DIAGONAL_13 = 0x0402010000000000L;
    public static final long DOWN_DIAGONAL_14 = 0x0201000000000000L;
    public static final long DOWN_DIAGONAL_15 = 0x0100000000000000L;

    public static final long[] DOWN_DIAGONALS = {
        DOWN_DIAGONAL_1, DOWN_DIAGONAL_2, DOWN_DIAGONAL_3, DOWN_DIAGONAL_4, DOWN_DIAGONAL_5, DOWN_DIAGONAL_6, DOWN_DIAGONAL_7, 
        DOWN_DIAGONAL_8,
        DOWN_DIAGONAL_9, DOWN_DIAGONAL_10, DOWN_DIAGONAL_11, DOWN_DIAGONAL_12, DOWN_DIAGONAL_13, DOWN_DIAGONAL_14, DOWN_DIAGONAL_15
    };

    public static final long[] DOWN_DIAGONALS_BOARD = new long[64];

    public static final long UP_DIAGONAL_1 = 0x0000000000000001L;
    public static final long UP_DIAGONAL_2 = 0x0000000000000102L;
    public static final long UP_DIAGONAL_3 = 0x0000000000010204L;
    public static final long UP_DIAGONAL_4 = 0x0000000001020408L;
    public static final long UP_DIAGONAL_5 = 0x0000000102040810L;
    public static final long UP_DIAGONAL_6 = 0x0000010204081020L;
    public static final long UP_DIAGONAL_7 = 0x0001020408102040L;
    public static final long UP_DIAGONAL_8 = 0x0102040810204080L;
    public static final long UP_DIAGONAL_9 = 0x0204081020408000L;
    public static final long UP_DIAGONAL_10 = 0x0408102040800000L;
    public static final long UP_DIAGONAL_11 = 0x0810204080000000L;
    public static final long UP_DIAGONAL_12 = 0x1020408000000000L;
    public static final long UP_DIAGONAL_13 = 0x2040800000000000L;
    public static final long UP_DIAGONAL_14 = 0x4080000000000000L;
    public static final long UP_DIAGONAL_15 = 0x8000000000000000L;

    public static final long[] UP_DIAGONALS = {
        UP_DIAGONAL_1, UP_DIAGONAL_2, UP_DIAGONAL_3, UP_DIAGONAL_4, UP_DIAGONAL_5, UP_DIAGONAL_6, UP_DIAGONAL_7,
        UP_DIAGONAL_8,
        UP_DIAGONAL_9, UP_DIAGONAL_10, UP_DIAGONAL_11, UP_DIAGONAL_12, UP_DIAGONAL_13, UP_DIAGONAL_14, UP_DIAGONAL_15
    };

    public static final long[] UP_DIAGONALS_BOARD = new long[64];


    public static final long[][] PAWN_MOVES = new long[2][64];

    public static final long[][] PAWN_ATTACKS = new long[2][64];

    public static final long[] KNIGHT_MOVES = new long[64];

    public static final long[] KING_MOVES = new long[64];
    public static final long[][] KING_CASTLE_EMPTY_SQUARES = new long[2][2];
    public static final long[][] KING_CASTLE_ATTACK_SQUARES = new long[2][2];

    public static final long[] BISHOP_MASKS = new long[64];
    public static final long[] ROOK_MASKS = new long[64];

    public static final long[] BISHOP_MAGIC_NUMBERS = new long[64];
    public static final long[] ROOK_MAGIC_NUMBERS = new long[64];

    public static final long[][] BISHOP_MOVES = new long[64][];
    public static final long[][] ROOK_MOVES = new long[64][];

    public static void initializeBitBoard() {
        initDiagonals();
        
        readMagicNumberFile();

        // findMagicNumbers(PieceType.BISHOP);
        // findMagicNumbers(PieceType.ROOK);

        for (int square = 0; square < 64; square ++) {
            initPawnMoves(square);
            initKnightMoves(square);
            initKingMoves(square);
            initBishopMask(square);
            initRookMask(square);

            initBishopMoves(square);
            initRookMoves(square);
        }
        initKingCastleMoves();
        // final int square = 0;
        // final int blockerPermutation = 24;
        // final long mask = BISHOP_MASKS[square];
        // final long blockers = generateBlockerPermutations(mask)[blockerPermutation];
        // final long moves = BISHOP_MOVES[square][(int) ((blockers * BISHOP_MAGIC_NUMBERS[square]) >>> (64 - Long.bitCount(mask)))];
        // displayBitboard(mask);
        // System.out.println();
        // displayBitboard(moves);
        // System.out.println();
        // displayBitboard(generateBlockerPermutations(mask)[blockerPermutation]);

    }

    private static void initDiagonals() {
        for (final long diagonal : DOWN_DIAGONALS) {
            applyFunctionByBitIndices(diagonal, (int index) -> DOWN_DIAGONALS_BOARD[index] = diagonal);
        }

        for (final long diagonal : UP_DIAGONALS) {
            applyFunctionByBitIndices(diagonal, (int index) -> UP_DIAGONALS_BOARD[index] = diagonal);
        }
    }

    private static void initPawnMoves(int square) {
        for (final PieceColor color : PIECE_COLORS) {
            final long b = 1L << square;
            long moves = 0;
            long attacks = 0;
            if (color == WHITE) {
                moves |= b >>> UP.absoluteArrayValue;
                moves |= (b & RANKS[PAWN_STARTING_ROW[color.arrayIndex]]) >>> UP.absoluteArrayValue * 2;
                attacks |= (b & ~FILE_A) >>> UPLEFT.absoluteArrayValue;
                attacks |= (b & ~FILE_H) >>> UPRIGHT.absoluteArrayValue;
            }
            else {
                moves |= b << DOWN.absoluteArrayValue;
                moves |= (b & RANKS[PAWN_STARTING_ROW[color.arrayIndex]]) << DOWN.absoluteArrayValue * 2;
                attacks |= (b & ~FILE_A) << DOWNLEFT.absoluteArrayValue;
                attacks |= (b & ~FILE_H) << DOWNRIGHT.absoluteArrayValue;
            }
            PAWN_MOVES[color.arrayIndex][square] = moves;
            PAWN_ATTACKS[color.arrayIndex][square] = attacks;
        }
    }

    private static void initKnightMoves(int square) {
        final long b = 1L << square;
        long attacks = 0;

        attacks |= (b & ~FILE_H) >>> sumDirectionsAbsolute(UP, UP, RIGHT);
        attacks |= (b & ~FILE_A) >>> sumDirectionsAbsolute(UP, UP, LEFT);
        attacks |= (b & ~FILE_H & ~ FILE_G) >>> sumDirectionsAbsolute(UP, RIGHT, RIGHT);
        attacks |= (b & ~FILE_A & ~FILE_B) >>> sumDirectionsAbsolute(UP, LEFT, LEFT);

        attacks |= (b & ~FILE_H) << sumDirectionsAbsolute(DOWN, DOWN, RIGHT);
        attacks |= (b & ~FILE_A) << sumDirectionsAbsolute(DOWN, DOWN, LEFT);
        attacks |= (b & ~FILE_H & ~ FILE_G) << sumDirectionsAbsolute(DOWN, RIGHT, RIGHT);
        attacks |= (b & ~FILE_A & ~FILE_B) << sumDirectionsAbsolute(DOWN, LEFT, LEFT);

        KNIGHT_MOVES[square] = attacks;
    }

    private static void initKingMoves(int square) {
        final long b = 1L << square;
        long attacks = 0;

        attacks |= (b & ~FILE_A) >>> UPLEFT.absoluteArrayValue;
        attacks |= (b & ~FILE_A) >>> LEFT.absoluteArrayValue;
        attacks |= (b & ~FILE_A) << DOWNLEFT.absoluteArrayValue;

        attacks |= (b & ~FILE_H) >>> UPRIGHT.absoluteArrayValue;
        attacks |= (b & ~FILE_H) << RIGHT.absoluteArrayValue;
        attacks |= (b & ~FILE_H) << DOWNRIGHT.absoluteArrayValue;

        attacks |= b >>> UP.absoluteArrayValue;
        attacks |= b << DOWN.absoluteArrayValue;

        KING_MOVES[square] = attacks;
    }

    private static void initKingCastleMoves() {

        KING_CASTLE_EMPTY_SQUARES[BLACK.arrayIndex][QUEENSIDE] = (1L << BLACK_KING_POS + LEFT.rawArrayValue) | (1L << BLACK_QUEENSIDE_CASTLE_SQUARE);
        KING_CASTLE_EMPTY_SQUARES[BLACK.arrayIndex][KINGSIDE] = (1L << BLACK_KING_POS + RIGHT.rawArrayValue) | (1L << BLACK_KINGSIDE_CASTLE_SQUARE);
        KING_CASTLE_EMPTY_SQUARES[WHITE.arrayIndex][QUEENSIDE] = (1L << WHITE_KING_POS + LEFT.rawArrayValue) | (1L << WHITE_QUEENSIDE_CASTLE_SQUARE);
        KING_CASTLE_EMPTY_SQUARES[WHITE.arrayIndex][KINGSIDE] = (1L << WHITE_KING_POS + RIGHT.rawArrayValue) | (1L << WHITE_KINGSIDE_CASTLE_SQUARE);

        KING_CASTLE_ATTACK_SQUARES[BLACK.arrayIndex][QUEENSIDE] = KING_CASTLE_EMPTY_SQUARES[BLACK.arrayIndex][QUEENSIDE] | (1L << BLACK_KING_POS);
        KING_CASTLE_ATTACK_SQUARES[BLACK.arrayIndex][KINGSIDE] = KING_CASTLE_EMPTY_SQUARES[BLACK.arrayIndex][KINGSIDE] | (1L << BLACK_KING_POS);
        KING_CASTLE_ATTACK_SQUARES[WHITE.arrayIndex][QUEENSIDE] = KING_CASTLE_EMPTY_SQUARES[WHITE.arrayIndex][QUEENSIDE] | (1L << WHITE_KING_POS);
        KING_CASTLE_ATTACK_SQUARES[WHITE.arrayIndex][KINGSIDE] = KING_CASTLE_EMPTY_SQUARES[WHITE.arrayIndex][KINGSIDE] | (1L << WHITE_KING_POS);

        KING_CASTLE_EMPTY_SQUARES[BLACK.arrayIndex][QUEENSIDE] |= squareToBitboard(BLACK_KING_POS + LEFT.rawArrayValue * 3);
        KING_CASTLE_EMPTY_SQUARES[WHITE.arrayIndex][QUEENSIDE] |= squareToBitboard(WHITE_KING_POS + LEFT.rawArrayValue * 3);
    }

    private static void initBishopMask(int square) {
        final long squareMask = 1L << square;
        long mask = 0L;

        for (int i = 1; i < getNumSquaresFromEdge(UPLEFT, square); i++) mask |= squareMask >>> UPLEFT.absoluteArrayValue * i;
        for (int i = 1; i < getNumSquaresFromEdge(UPRIGHT, square); i++) mask |= squareMask >>> UPRIGHT.absoluteArrayValue * i;
        for (int i = 1; i < getNumSquaresFromEdge(DOWNLEFT, square); i++) mask |= squareMask << DOWNLEFT.absoluteArrayValue * i;
        for (int i = 1; i < getNumSquaresFromEdge(DOWNRIGHT, square); i++) mask |= squareMask << DOWNRIGHT.absoluteArrayValue * i;

        BISHOP_MASKS[square] = mask;
    }

    private static void initRookMask(int square) {
        final int rank = getRow(square);
        final int file = getColumn(square);
        long mask = RANKS[rank] ^ FILES[file];

        if (rank != 0)
            mask = mask & ~RANKS[0]; // Exclude rank 1
        if (rank != 7)  
            mask = mask & ~RANKS[7]; // Exclude rank 8
        if (file != 0)
            mask = mask & ~FILES[0]; // Exclude file A
        if (file != 7)
            mask = mask & ~FILES[7]; // Exclude file H

        ROOK_MASKS[square] = mask;
    }

    private static void initBishopMoves(int square) {
        final long mask = BISHOP_MASKS[square];
        final long[] blockerPermutations = generateBlockerPermutations(mask);

        final int numRelevantSquares = Long.bitCount(mask);
        final long[] attackTable = new long[1 << numRelevantSquares];
        final long magicNumber = BISHOP_MAGIC_NUMBERS[square];

        final HashSet<Integer> used = new HashSet<Integer>();

        for (final long blockerPermutation : blockerPermutations) {
            final int index = (int)(((blockerPermutation) * magicNumber) >>> (64 - numRelevantSquares));
            if (!used.add(index)) {
                System.out.println("Bishop error on square " + square);
            }
            attackTable[index] = generateAttacksFrom(DIAGONAL_DIRECTIONS, blockerPermutation, square);
        }
        BISHOP_MOVES[square] = attackTable;
    }

    private static void initRookMoves(int square) {
        final long mask = ROOK_MASKS[square];
        final long[] blockerPermutations = generateBlockerPermutations(mask);

        final int numRelevantSquares = Long.bitCount(mask);
        final long[] attackTable = new long[1 << numRelevantSquares];
        final long magicNumber = ROOK_MAGIC_NUMBERS[square];

        final HashSet<Integer> used = new HashSet<Integer>();

        for (final long blockerPermutation : blockerPermutations) {
            final int index = (int)(((blockerPermutation) * magicNumber) >>> (64 - numRelevantSquares));
            if (!used.add(index)) {
                System.out.println("Rook error on square " + square);
            }
            attackTable[index] = generateAttacksFrom(STRAIGHT_DIRECTIONS, blockerPermutation, square);
        }
        ROOK_MOVES[square] = attackTable;
    }

    private static void readMagicNumberFile() {
        try (DataInputStream in = new DataInputStream(new FileInputStream("Chess//BISHOP_MAGIC_NUMBERS.bin"))) {
            for (int i = 0; i < 64; i++) {
                BISHOP_MAGIC_NUMBERS[i] = in.readLong();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // System.out.println(Arrays.toString(BISHOP_MAGIC_NUMBERS));

        try (DataInputStream in = new DataInputStream(new FileInputStream("Chess//ROOK_MAGIC_NUMBERS.bin"))) {
            for (int i = 0; i < 64; i++) {
                ROOK_MAGIC_NUMBERS[i] = in.readLong();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // System.out.println(Arrays.toString(ROOK_MAGIC_NUMBERS));
    }

    private static void findMagicNumbers(PieceType type) {
        final long[] MAGIC_NUMBERS = type == PieceType.BISHOP ? BISHOP_MAGIC_NUMBERS : ROOK_MAGIC_NUMBERS;
        for (int square = 0; square < 64; square++) {
            if (MAGIC_NUMBERS[square] != 0) {
                continue;
            }
            MAGIC_NUMBERS[square] = findMagicNumber(square, type);
        }

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream("Chess//" + type.name() + "_MAGIC_NUMBERS.bin"))) {
            for (long magic : MAGIC_NUMBERS) {
                out.writeLong(magic);
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static long findMagicNumber(int square, PieceType type) {
        final long masks[] = type == PieceType.BISHOP ? BISHOP_MASKS : ROOK_MASKS;
        final long[] blockerPermutations = generateBlockerPermutations(masks[square]);
        final Random random = new Random();
        int count = 0;
        while (true) {
            long magic = random.nextLong() & random.nextLong() & random.nextLong();
            if (isMagic(blockerPermutations, masks[square], magic)) {
                System.out.println("Magic number found for " + type + " on square " + square + ": " + magic);
                return magic;
            }
            count ++;

            if (count % 10000000 == 0) System.out.println(count);
        }
    }

    private static boolean isMagic(long[] blockerPermutations, long mask, long magic) {
        Set<Integer> used = new HashSet<>();
        for (final long blockers : blockerPermutations) {
            int index = (int) ((blockers * magic) >>> (64 - Long.bitCount(mask)));
            if (!used.add(index)) {
                return false; // collision
            }
        }
        return true; // perfect hash
    }


    private static long[] generateBlockerPermutations(long mask) {
        final int numRelevantSquares = Long.bitCount(mask);
        final int numPermutations = 1 << numRelevantSquares;
        final long[] permutations = new long[numPermutations];

        final ArrayList<Integer> bitIndices = getBitIndices(mask);

        for (int i = 0; i < numPermutations; i++) {
            long blockers = 0L;
            for (int j = 0; j < numRelevantSquares; j++) {
                if (((i >>> j) & 1) != 0) {
                    blockers |= 1L << bitIndices.get(j);
                }
            }
            permutations[i] = blockers;
        }
        return permutations;
    }

    private static long generateAttacksFrom(Direction[] directions, long blockers, int square) {
        long attacks = 0L;

        for (final Direction direction : directions) {
            for (int delta = 1; delta < getNumSquaresFromEdge(direction, square) + 1; delta ++) {
                final int newSquare = square + direction.rawArrayValue * delta;
                final long mask = (1L) << newSquare;
                attacks |= mask;
                if ((blockers & mask) != 0) break;
            }
        }

        return attacks;
    }

    public static long generatePawnBitboard(PieceColor color, long validAttackSquares, long occupiedSquares, int pawnSquare, boolean attacksOnly) {
        final long pawnAttacks = PAWN_ATTACKS[color.arrayIndex][pawnSquare] & validAttackSquares;
        if (attacksOnly) return pawnAttacks;
        long pawnMoves = PAWN_MOVES[color.arrayIndex][pawnSquare] & ~occupiedSquares;
        if (getRow(pawnSquare) == PAWN_STARTING_ROW[color.arrayIndex]) {
            final int numMoves = Long.bitCount(pawnMoves);
            if (numMoves == 1) {
                pawnMoves &= ~(color == PieceColor.WHITE ? RANK5 : RANK4);
            }
        }
        return pawnAttacks | pawnMoves;
    }

    public static long generateKnightBitboard(long validSquares, int knightSquare) {
        final long knightMoves = KNIGHT_MOVES[knightSquare];
        return knightMoves & validSquares;
    }

    public static long generateBishopBitboard(long validSquares, long occupiedSquares, int bishopSquare) {
        final long bishopMask = BISHOP_MASKS[bishopSquare];
        final long blockers = bishopMask & occupiedSquares;
        final long bishopMagicNumber = BISHOP_MAGIC_NUMBERS[bishopSquare];
        final long bishopMoves = BISHOP_MOVES[bishopSquare][(int) ((blockers * bishopMagicNumber) >>> (64 - Long.bitCount(bishopMask)))];
        return bishopMoves & validSquares;
    }

    public static long generateRookBitboard(long validSquares, long occupiedSquares, int rookSquare) {
        final long rookMask = ROOK_MASKS[rookSquare];
        final long blockers = rookMask & occupiedSquares;
        final long rookMagicNumber = ROOK_MAGIC_NUMBERS[rookSquare];
        final long rookMoves = ROOK_MOVES[rookSquare][(int) ((blockers * rookMagicNumber) >>> (64 - Long.bitCount(rookMask)))];
        return rookMoves & validSquares;
    }

    public static long generateKingBitboard(long validSquares, int kingSquare) {
        final long kingMoves = KING_MOVES[kingSquare];
        return kingMoves & validSquares;
    }

    public static long generatePawnBitboardOneSquare(PieceColor color, long pawns, long occupiedBitboard) {
        if (color == PieceColor.WHITE) {
            return pawns >>> UP.absoluteArrayValue & ~occupiedBitboard;
        }
        return pawns << DOWN.absoluteArrayValue & ~occupiedBitboard;
    }

    public static long generatePawnBitboardTwoSquares(PieceColor color, long pawnBitboardOneSquare, long occupiedBitboard) {
        final long validPawns = pawnBitboardOneSquare & (color == PieceColor.WHITE ? RANK6 : RANK3);

        final long twoSpacesAhead = (color == PieceColor.WHITE ? validPawns >>> UP.absoluteArrayValue : validPawns << DOWN.absoluteArrayValue);
        return twoSpacesAhead & ~occupiedBitboard;
    }

    public static long generatePawnBitboardAttacksLeft(PieceColor color, long pawns, long opposingPieces) {
        final long filteredPawns = pawns & ~FILES[0]; // Exclude leftmost file to avoid overflow

        final long leftAttacks = color == PieceColor.WHITE ? filteredPawns >>> UPLEFT.absoluteArrayValue : filteredPawns << DOWNLEFT.absoluteArrayValue;

        return leftAttacks & opposingPieces;
    }

    public static long generatePawnBitboardAttacksRight(PieceColor color, long pawns, long opposingPieces) {
        final long filteredPawns = pawns & ~FILES[7]; // Exclude rightmost file to avoid overflow

        final long rightAttacks = color == PieceColor.WHITE ? filteredPawns >>> UPRIGHT.absoluteArrayValue : filteredPawns << DOWNRIGHT.rawArrayValue;

        return rightAttacks & opposingPieces;
    }

    public static long generatePawnBitboardEnPassant(PieceColor color, long pawns, int enPassantSquare) {
        if (enPassantSquare == EMPTY) return 0L;
        final long epBitBoard = squareToBitboard(enPassantSquare);
        final long leftPawn = (epBitBoard & ~FILES[0]) >>> LEFT.absoluteArrayValue;
        final long rightPawn = (epBitBoard & ~FILES[7]) << RIGHT.absoluteArrayValue;
        return (leftPawn | rightPawn) & pawns;
    }

    public static boolean sacrificesKing(PieceType pinType, long pinningPieces, long occupiedBitboard, long kingBitboard) {
        if (pinType == PieceType.ROOK) {
            while (pinningPieces != 0) {
                final int attackerPos = Long.numberOfTrailingZeros(pinningPieces);
                final long attackerMoves = generateRookBitboard(kingBitboard, occupiedBitboard, attackerPos);
                if (attackerMoves != 0) return true;
                pinningPieces &= pinningPieces - 1;
            }
        }
        else {
            while (pinningPieces != 0) {
                final int attackerPos = Long.numberOfTrailingZeros(pinningPieces);
                final long attackerMoves = generateBishopBitboard(kingBitboard, occupiedBitboard, attackerPos);
                if (attackerMoves != 0) return true;
                pinningPieces &= pinningPieces - 1;
            }
        }
        return false;
    }

    public static PieceType getPinType(long[] pieces, long occupied, long validSquares) {
        long diagonalAttackers = pieces[PieceType.BISHOP.arrayIndex] | pieces[PieceType.QUEEN.arrayIndex];
        while (diagonalAttackers != 0) {
            final int attackerPos = Long.numberOfTrailingZeros(diagonalAttackers);
            final long attackerMoves = generateBishopBitboard(validSquares, occupied, attackerPos);
            if (attackerMoves != 0) return PieceType.BISHOP;
            diagonalAttackers &= diagonalAttackers - 1;
        }

        long straightAttackers = pieces[PieceType.ROOK.arrayIndex] | pieces[PieceType.QUEEN.arrayIndex];
        while (straightAttackers != 0) {
            final int attackerPos = Long.numberOfTrailingZeros(straightAttackers);
            final long attackerMoves = generateRookBitboard(validSquares, occupied, attackerPos);
            if (attackerMoves != 0) return PieceType.ROOK;
            straightAttackers &= straightAttackers - 1;
        }
        return PieceType.EMPTY;
    }

    public static boolean isAttacked(PieceColor color, long[] pieces, long occupied, long validSquares) {
        return getAttackerType(color, pieces, occupied, validSquares) != PieceType.EMPTY;
    }

    public static PieceType getAttackerType(PieceColor color, long[] pieces, long occupied, long validSquares) {
        final PieceColor opposingColor = flipColor(color);

        //Pawns
        final long pawns = pieces[PieceType.PAWN.arrayIndex];
        final long pawnAttacks = generatePawnBitboardAttacksLeft(opposingColor, pawns, validSquares) | generatePawnBitboardAttacksRight(opposingColor, pawns, validSquares);
        if (pawnAttacks != 0) return PieceType.PAWN;

        //Knights
        long knights = pieces[PieceType.KNIGHT.arrayIndex];
        while (knights != 0) {
            final int knightPos = Long.numberOfTrailingZeros(knights);
            final long knightMoves = generateKnightBitboard(validSquares, knightPos);
            if (knightMoves != 0) return PieceType.KNIGHT;
            knights &= knights - 1;
        }

        long king = pieces[PieceType.KING.arrayIndex];
        while (king != 0) {
            final int kingPos = Long.numberOfTrailingZeros(king);
            final long kingMoves = generateKingBitboard(validSquares, kingPos);
            if (kingMoves != 0) return PieceType.KING;
            king &= king - 1;
        }

        long diagonalAttackers = pieces[PieceType.BISHOP.arrayIndex] | pieces[PieceType.QUEEN.arrayIndex];
        while (diagonalAttackers != 0) {
            final int attackerPos = Long.numberOfTrailingZeros(diagonalAttackers);
            final long attackerMoves = generateBishopBitboard(validSquares, occupied, attackerPos);
            if (attackerMoves != 0) return PieceType.BISHOP;
            diagonalAttackers &= diagonalAttackers - 1;
        }

        long straightAttackers = pieces[PieceType.ROOK.arrayIndex] | pieces[PieceType.QUEEN.arrayIndex];
        while (straightAttackers != 0) {
            final int attackerPos = Long.numberOfTrailingZeros(straightAttackers);
            final long attackerMoves = generateRookBitboard(validSquares, occupied, attackerPos);
            if (attackerMoves != 0) return PieceType.ROOK;
            straightAttackers &= straightAttackers - 1;
        }

        return PieceType.EMPTY;
    }


    public static long setBit(long bitboard, int index) {
        return bitboard |= (1L << index);
    }

    public static long clearBit(long bitboard, int index) {
        bitboard &= ~(1L << index);
        return bitboard;
    }

    public static long squareToBitboard(int index) {
        return 1L << index;
    }

    public static int firstBitToIndex(long bitboard) {
        return Long.numberOfTrailingZeros(bitboard);
    }

    public static boolean isBitSet(long bitboard, int index) {
        return (bitboard & (1L << index)) != 0;
    }

    public static PieceType charToPieceType(char letter) {
		for (final PieceType pieceType : PIECE_TYPES) {
			if (Character.toLowerCase(letter) == pieceType.characterRepresentation) {
				return pieceType;
			}
		}
		throw new IllegalArgumentException("Invalid piece character");
	}

    public static void applyFunctionByBitIndices(long bitboard, IntConsumer consumer) {
        while (bitboard != 0) {
            final int index = Long.numberOfTrailingZeros(bitboard);
            consumer.accept(index);
            bitboard &= bitboard - 1; // Clear the LSB
        }
    }

    public static ArrayList<Integer> getBitIndices(long bitboard) {
        final ArrayList<Integer> indices = new ArrayList<>();
        applyFunctionByBitIndices(bitboard, indices::add);
        return indices;
    }

    public static void displayBitboard(long bitboard) {
        for(int row = 0; row < 8; row ++) {
            for (int col = 0; col < 8; col++) {
                final long mask = (1L) << (row * 8 + col);
                System.out.print(((bitboard & mask) != 0 ? "1" : "0") + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
