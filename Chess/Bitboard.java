package Chess;

import java.util.ArrayList;
import java.util.Arrays;
import static Chess.Constants.DirectionConstants.Direction.*;
import static Chess.Constants.PieceConstants.PIECE_COLORS;
import static Chess.BitboardHelper.*;

import Chess.Constants.PieceConstants.PieceColor;
import Chess.Constants.PieceConstants.PieceType;

import static Chess.Constants.PositionConstants.*;
import static Chess.BoardUtil.*;

public class Bitboard {

    public final long[] PIECES;

    public final long[] ALL_PIECES;

    private final ChessPiece[] board;

    private long OCCUPIED;

    public final long[] ATTACKS;

    private final PieceSet[][] allAttackers;

    private int enPassantSquare;

    private final boolean[][] castlingRights;

    public Bitboard(String fen) {
        PIECES = new long[12];
        ALL_PIECES = new long[2];
        board = new ChessPiece[64];
        OCCUPIED = 0L;
        ATTACKS = new long[2];

        allAttackers = new PieceSet[2][64];
        for (final PieceColor color : PIECE_COLORS) {
            final PieceSet[] attackerSquares = new PieceSet[64];
            for (int i = 0; i < 64; i ++) {
                attackerSquares[i] = new PieceSet();
            }
            allAttackers[color.arrayIndex] = attackerSquares;
        }

        enPassantSquare = EMPTY;
        castlingRights = new boolean[2][2]; //Black: Queenside, Kingside, White: Queenside, Kingside
		Arrays.fill(castlingRights[PieceColor.BLACK.arrayIndex], false);
		Arrays.fill(castlingRights[PieceColor.WHITE.arrayIndex], false);

        Arrays.fill(PIECES, 0L);
        Arrays.fill(ALL_PIECES, 0L);
        for (int i = 0; i < 64; i ++) {
            board[i] = ChessPiece.empty();
        }

        initializeFromFen(fen);
        hardAttackUpdate();
    }


    public void initializeFromFen(String fen) {
        int pos = 0;
        final int[] pieceIDs = new int[] {0, 0};	//Piece IDs to be used for discount Hash map.
		//Iterate over each character in the FEN String.
		for (int index = 0; index < fen.length(); index++) {
            if (pos < 64) {
                final char letter = fen.charAt(index);
                if (letter == ' ') continue;
                
                //Turn, Castling, enPassant.
                
                if (letter == '/') continue;

                //Filling board with pieces.
                final int pieceValue = Character.getNumericValue(letter);
                //Empty squares.
                if (pieceValue <= 8 && pieceValue > 0) {
                    for (int square = 0; square < pieceValue; square++) {
                        pos ++;
                    }
                    continue;
                }

                //Create and store the piece.
                // final PieceType type = charToPieceType(letter);
                // final PieceColor color = Character.isLowerCase(letter) ? PieceColor.BLACK : PieceColor.WHITE;
                // final ChessPiece piece = new ChessPiece(type, color, pos, null, pieceValue)
                final ChessPiece piece = charToPiece(letter, pos, null, pieceIDs);

                setPiece(pos, piece);

                pos++;
            }
		}

    }

    public void generatePawnMoves(ArrayList<Move> moves, PieceColor color, int square, boolean attacksOnly) {
        final long pawnBitboard = generatePawnBitboard(color, square, attacksOnly);
        applyFunctionByBitIndices(pawnBitboard, (int index) -> moves.add(new Move(square, index)));
    }

    public void generateKnightMoves(ArrayList<Move> moves, PieceColor color, int square, boolean attacksOnly) {
        final long knightBitboard = generateKnightBitboard(color, square, attacksOnly);
        applyFunctionByBitIndices(knightBitboard, (int index) -> moves.add(new Move(square, index)));
    }

    public void generateBishopMoves(ArrayList<Move> moves, PieceColor color, int square, boolean attacksOnly) {
        final long bishopBitboard = generateBishopBitboard(color, square, attacksOnly);
        applyFunctionByBitIndices(bishopBitboard, (int index) -> moves.add(new Move(square, index)));
    }

    public void generateRookMoves(ArrayList<Move> moves, PieceColor color, int square, boolean attacksOnly) {
        final long rookBitboard = generateRookBitboard(color, square, attacksOnly);
        applyFunctionByBitIndices(rookBitboard, (int index) -> moves.add(new Move(square, index)));
    }

    public void generateQueenMoves(ArrayList<Move> moves, PieceColor color, int square, boolean attacksOnly) {
        final long queenBitboard = generateQueenBitboard(color, square, attacksOnly);
        applyFunctionByBitIndices(queenBitboard, (int index) -> moves.add(new Move(square, index)));
    }

    public void generateKingMoves(ArrayList<Move> moves, PieceColor color, int square, boolean attacksOnly) {
        final long kingBitboard = generateKingBitboard(color, square, attacksOnly);
        applyFunctionByBitIndices(kingBitboard, (int index) -> moves.add(new Move(square, index)));
    }

    public long generatePawnBitboard(PieceColor color, int square, boolean attacksOnly) {
        final long pawnAttacks = PAWN_ATTACKS[color.arrayIndex][square] & ALL_PIECES[flipColor(color).arrayIndex];
        if (attacksOnly) return pawnAttacks;
        long pawnMoves = PAWN_MOVES[color.arrayIndex][square] & ~OCCUPIED;
        if (getRow(square) == PAWN_STARTING_ROW[color.arrayIndex]) {
            final int numMoves = Long.bitCount(pawnMoves);
            if (numMoves == 1) {
                pawnMoves &= ~(color == PieceColor.WHITE ? RANK6 : RANK4);
            }
        }
        return pawnAttacks | pawnMoves;
    }

    public long generateKnightBitboard(PieceColor color, int square, boolean attacksOnly) {
        final long knightMoves = KNIGHT_MOVES[square];
        final long viableSquares = ALL_PIECES[flipColor(color).arrayIndex] | (!attacksOnly ? ~OCCUPIED : 0L);
        return knightMoves & viableSquares;
    }

    public long generateBishopBitboard(PieceColor color, int square, boolean attacksOnly) {
        final long bishopMask = BISHOP_MASKS[square];
        final long blockers = bishopMask & OCCUPIED;
        final long bishopMagicNumber = BISHOP_MAGIC_NUMBERS[square];
        final long bishopMoves = BISHOP_MOVES[square][(int) ((blockers * bishopMagicNumber) >>> (64 - Long.bitCount(bishopMask)))];
        final long viableSquares = ALL_PIECES[flipColor(color).arrayIndex] | (!attacksOnly ? ~OCCUPIED : 0L);
        return bishopMoves & viableSquares;
    }

    public long generateRookBitboard(PieceColor color, int square, boolean attacksOnly) {
        final long rookMask = ROOK_MASKS[square];
        final long blockers = rookMask & OCCUPIED;
        final long rookMagicNumber = ROOK_MAGIC_NUMBERS[square];
        final long rookMoves = ROOK_MOVES[square][(int) ((blockers * rookMagicNumber) >>> (64 - Long.bitCount(rookMask)))];
        final long viableSquares = ALL_PIECES[flipColor(color).arrayIndex] | (!attacksOnly ? ~OCCUPIED : 0L);
        return rookMoves & viableSquares;
    }

    public long generateQueenBitboard(PieceColor color, int square, boolean attacksOnly) {
        return generateBishopBitboard(color, square, attacksOnly) | generateRookBitboard(color, square, attacksOnly);
    }

    public long generateKingBitboard(PieceColor color, int square, boolean attacksOnly) {
        final long possibleKingMoves = KING_MOVES[square];
        final long viableSquares = ALL_PIECES[flipColor(color).arrayIndex] | (!attacksOnly ? ~OCCUPIED : 0L);
        long kingMoves = possibleKingMoves & viableSquares;

        if (attacksOnly) return kingMoves;

        if (canCastle(color, square, QUEENSIDE)) kingMoves |= KING_CASTLE_MOVES[color.arrayIndex][QUEENSIDE];
        if (canCastle(color, square, KINGSIDE)) kingMoves |= KING_CASTLE_MOVES[color.arrayIndex][KINGSIDE];

        return kingMoves;
    }

    private boolean canCastle(PieceColor color, int square, int side) {
        if (!castlingRights[color.arrayIndex][side]) return false;

        final ChessPiece castlingRook = board[ROOK_POSITIONS[color.arrayIndex][side]];
        if (!castlingRook.isRook() || castlingRook.getColor() != color) return false;

        if ((KING_CASTLE_EMPTY_SQUARES[color.arrayIndex][side] & OCCUPIED) != 0) return false;

        if ((KING_CASTLE_ATTACK_SQUARES[color.arrayIndex][side] & ATTACKS[flipColor(color).arrayIndex]) != 0) return false;

        return true;
    }

    public static int[][] pawnIndexOffsets = {
        {
            UP.rawArrayValue,
            UP.rawArrayValue * 2,
            UPRIGHT.rawArrayValue,
            UPLEFT.rawArrayValue
        },
        {
            DOWN.rawArrayValue,
            DOWN.rawArrayValue * 2,
            DOWNRIGHT.rawArrayValue,
            DOWNLEFT.rawArrayValue, 
        }
    };

    public void generateAllPawnMoves(ArrayList<Move> moves, PieceColor color) {
        final long pawnBitboardOneSquare = generatePawnBitboardOneSquare(color);
        final long pawnBitboardTwoSquares = generatePawnBitboardTwoSquares(color, pawnBitboardOneSquare);
        final long pawnBitboardAttacksLeft = generatePawnBitboardAttacksLeft(color);
        final long pawnBitboardAttacksRight = generatePawnBitboardAttacksRight(color);
        final long pawnBitboardEnPassant = generatePawnBitboardEnPassant(color);

        final int[] pawnOffsets = pawnIndexOffsets[color.arrayIndex];

        applyFunctionByBitIndices(pawnBitboardOneSquare, (int index) -> moves.add(new Move(index + pawnOffsets[0], index)));
        applyFunctionByBitIndices(pawnBitboardTwoSquares, (int index) -> moves.add(new Move(index + pawnOffsets[1], index)));
        applyFunctionByBitIndices(pawnBitboardAttacksLeft, (int index) -> moves.add(new Move(index + pawnOffsets[2], index)));
        applyFunctionByBitIndices(pawnBitboardAttacksRight, (int index) -> moves.add(new Move(index + pawnOffsets[3], index)));
        applyFunctionByBitIndices(pawnBitboardEnPassant, (int index) -> moves.add(new Move(index, enPassantSquare)));
    }

    private long generatePawnBitboardOneSquare(PieceColor color) {
        final long pawns = PIECES[PieceType.PAWN.arrayIndex + color.arrayIndex * 6];
        if (color == PieceColor.WHITE) {
            return pawns >>> UP.absoluteArrayValue & ~OCCUPIED;
        }
        return pawns << DOWN.absoluteArrayValue & ~OCCUPIED;
    }

    private long generatePawnBitboardTwoSquares(PieceColor color, long pawnBitboardOneSquare) {
        final long pawns = PIECES[PieceType.PAWN.arrayIndex + color.arrayIndex * 6];
        final long startingRow = RANKS[PAWN_STARTING_ROW[color.arrayIndex]];

        final long pawnsOnStartingRow = pawns & startingRow;

        final long pawnsThatCanMoveOneSquare = pawns & (color == PieceColor.WHITE ? pawnBitboardOneSquare << DOWN.absoluteArrayValue : pawnBitboardOneSquare >>> UP.absoluteArrayValue);
        final long potentialPawns = (pawnsThatCanMoveOneSquare & pawnsOnStartingRow);

        final long twoSpacesAhead = color == PieceColor.WHITE ? potentialPawns >>> UP.absoluteArrayValue * 2 : potentialPawns << DOWN.absoluteArrayValue * 2;
        return twoSpacesAhead & ~OCCUPIED;
    }

    private long generatePawnBitboardAttacksLeft(PieceColor color) {
        final long pawns = PIECES[PieceType.PAWN.arrayIndex + color.arrayIndex * 6];
        final long filteredPawns = pawns & ~FILES[0]; // Exclude leftmost file to avoid overflow

        final long leftAttacks = color == PieceColor.WHITE ? filteredPawns >>> UPLEFT.absoluteArrayValue : filteredPawns << DOWNLEFT.absoluteArrayValue;

        final long opposingPieces = ALL_PIECES[flipColor(color).arrayIndex];
        return leftAttacks & opposingPieces;
    }

    private long generatePawnBitboardAttacksRight(PieceColor color) {
        final long pawns = PIECES[PieceType.PAWN.arrayIndex + color.arrayIndex * 6];
        final long filteredPawns = pawns & ~FILES[7]; // Exclude rightmost file to avoid overflow

        final long rightAttacks = color == PieceColor.WHITE ? filteredPawns >>> UPRIGHT.absoluteArrayValue : filteredPawns << DOWNRIGHT.rawArrayValue;

        final long opposingPieces = ALL_PIECES[flipColor(color).arrayIndex];
        return rightAttacks & opposingPieces;
    }

    private long generatePawnBitboardEnPassant(PieceColor color) {
        if (enPassantSquare == EMPTY) return 0L;
        final long epBitBoard = 1L << enPassantSquare;
        final long leftPawn = (epBitBoard & ~FILES[0]) >>> LEFT.absoluteArrayValue;
        final long rightPawn = (epBitBoard & ~FILES[7]) << RIGHT.absoluteArrayValue;
        final long pawns = PIECES[PieceType.PAWN.arrayIndex + color.arrayIndex * 6];
        return (leftPawn | rightPawn) & pawns;
    }

    public void hardAttackUpdate() {
        Arrays.fill(ATTACKS, 0L);

        for (final PieceColor color : PIECE_COLORS) {
            final PieceSet[] attackerSquares = new PieceSet[64];
            for (int i = 0; i < 64; i ++) {
                attackerSquares[i] = new PieceSet();
            }
            allAttackers[color.arrayIndex] = attackerSquares;
        }

        for (final ChessPiece piece : board) {
            if (piece.isEmpty()) continue;
            updateAttacks(piece, false);
        }
    }

    public void updateAttacks(ChessPiece piece, boolean remove) {
        updateAttacks(piece, remove, OCCUPIED);
    }

    private void updateAttacks(ChessPiece piece, boolean remove, long occupiedBitboard) {
        long bitboard = 0;
        if (piece.getType() == PieceType.PAWN) {
            bitboard = PAWN_ATTACKS[piece.getColor().arrayIndex][piece.getPos()];
        }
        if (piece.getType() == PieceType.KNIGHT) {
            bitboard = KNIGHT_MOVES[piece.getPos()];
        }
        if (piece.getType() == PieceType.KING) {
            bitboard = KING_MOVES[piece.getPos()];
        }
        if (piece.getType() == PieceType.BISHOP || piece.getType() == PieceType.QUEEN) {
            final long mask = BISHOP_MASKS[piece.getPos()];
            final long blockers = mask & occupiedBitboard;
            final long magicNumber = BISHOP_MAGIC_NUMBERS[piece.getPos()];
            bitboard |= BISHOP_MOVES[piece.getPos()][(int) ((blockers * magicNumber) >>> (64 - Long.bitCount(mask)))];
        }
        if (piece.getType() == PieceType.ROOK || piece.getType() == PieceType.QUEEN) {
            final long mask = ROOK_MASKS[piece.getPos()];
            final long blockers = mask & occupiedBitboard;
            final long magicNumber = ROOK_MAGIC_NUMBERS[piece.getPos()];
            bitboard |= ROOK_MOVES[piece.getPos()][(int) ((blockers * magicNumber) >>> (64 - Long.bitCount(mask)))];
        }
        if (remove) applyFunctionByBitIndices(bitboard, (index) -> removeAttack(piece, index));
        else {
            ATTACKS[piece.getColor().arrayIndex] |= bitboard;
            applyFunctionByBitIndices(bitboard, (index) -> allAttackers[piece.getColor().arrayIndex][index].add(piece));
        }
    }

    private void removeAttack(ChessPiece piece, int index) {
        allAttackers[piece.getColor().arrayIndex][index].remove(piece);
        if (allAttackers[piece.getColor().arrayIndex][index].size() == 0) {
            ATTACKS[piece.getColor().arrayIndex] &= ~(1L << index);
        }
    }

    public void setPiece(int index, ChessPiece piece) {
        if (index < 0 || index >= 64) {
            throw new IllegalArgumentException("Index must be between 0 and 63.");
        }
        if (piece.isEmpty()) {
            throw new IllegalArgumentException("Invalid piece type or color.");
        }

        final boolean wasOccupied = isOccupied(index);

        final long prevOccupiedBitboard = OCCUPIED;

        if (wasOccupied) clearPiece(index, false);

        board[index] = piece;
        piece.setPos(index);

        updateAttacks(piece, false);

        PIECES[piece.getType().arrayIndex + piece.getColor().arrayIndex * 6] = setBit(PIECES[piece.getType().arrayIndex + piece.getColor().arrayIndex * 6], index);
        ALL_PIECES[piece.getColor().arrayIndex] = setBit(ALL_PIECES[piece.getColor().arrayIndex], index);
        OCCUPIED = setBit(OCCUPIED, index);

        if (wasOccupied) return;
        for (final PieceSet[] attackerSquares : allAttackers) {
            final PieceSet attackers = attackerSquares[index];
            for (final ChessPiece attackingPiece : attackers) {
                if (!(attackingPiece.isBishop() || attackingPiece.isRook() || attackingPiece.isQueen())) continue;
                updateAttacks(attackingPiece, true, prevOccupiedBitboard);
                updateAttacks(attackingPiece, false);
            }
        }
    }

    public void clearPiece(int index) {
        clearPiece(index, true);
    }

    public void clearPiece(int index, boolean updateAttackers) {
        if (index < 0 || index >= 64) {
            throw new IllegalArgumentException("Index must be between 0 and 63.");
        }

        final ChessPiece piece = board[index];

        updateAttacks(piece, true);
        PIECES[piece.getType().arrayIndex + piece.getColor().arrayIndex * 6] = clearBit(PIECES[piece.getType().arrayIndex + piece.getColor().arrayIndex * 6], index);
        ALL_PIECES[piece.getColor().arrayIndex] = clearBit(ALL_PIECES[piece.getColor().arrayIndex], index);
        OCCUPIED = clearBit(OCCUPIED, index);

        board[index] = ChessPiece.empty();

        if (!updateAttackers) return;
        for (final PieceSet[] attackerSquares : allAttackers) {
            final PieceSet attackers = attackerSquares[index];
            for (final ChessPiece attackingPiece : attackers) {
                if (!(attackingPiece.isBishop() || attackingPiece.isRook() || attackingPiece.isQueen())) continue;
                updateAttacks(attackingPiece, false);
            }
        }
    }

    public boolean isOccupied(int index) {
        return (OCCUPIED & (1L << index)) != 0;
    }

    public boolean[] getCastlingRights(PieceColor color) {
        return castlingRights[color.arrayIndex];
    }

    public boolean getCastlingRights(PieceColor color, int side) {
        return castlingRights[color.arrayIndex][side];
    }

    public int getEnPassant() {
        return enPassantSquare;
    }

    public void setEnPassant(int square) {
        enPassantSquare = square;
    }

    public void clearCastlingRights(PieceColor color) {
        Arrays.fill(castlingRights[color.arrayIndex], false);
    }

    public void setCastlingRights(PieceColor color, boolean[] values) {
        castlingRights[color.arrayIndex] = values;
    }

    public void setCastlingRights(PieceColor color, int side, boolean value) {
        castlingRights[color.arrayIndex][side] = value;
    }

}
