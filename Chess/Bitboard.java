package Chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static Chess.Constants.DirectionConstants.Direction.*;
import static Chess.Constants.PieceConstants.NON_KING_PIECES;
import static Chess.BitboardHelper.*;

import Chess.Constants.PieceConstants.PieceColor;
import Chess.Constants.PieceConstants.PieceType;

import static Chess.Constants.PositionConstants.*;
import static Chess.BoardUtil.*;

public class Bitboard {

    private final long[][] PIECES;

    private final long[] ALL_PIECES;

    private final ChessPiece[] board;

    private final PieceSet[] pieces;

    private long OCCUPIED;

    private int enPassantSquare;

    private final boolean[][] castlingRights;

    private boolean isChecked;

    public Bitboard(String fen) {
        PIECES = new long[2][6];
        ALL_PIECES = new long[2];
        board = new ChessPiece[64];
        pieces = new PieceSet[2];
        pieces[0] = new PieceSet();
        pieces[1] = new PieceSet();
        OCCUPIED = 0L;

        enPassantSquare = EMPTY;
        castlingRights = new boolean[2][2]; //Black: Queenside, Kingside, White: Queenside, Kingside
        
        Arrays.fill(ALL_PIECES, 0L);
        for (int i = 0; i < 64; i ++) {
            board[i] = ChessPiece.empty();
        }

        initializeFromFen(fen);
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
            else break;
		}
    }

    public void generatePieceMoves(List<Move> moves, int index, boolean attacksOnly) {
        generatePieceMoves((Move move) -> moves.add(move), index, attacksOnly);
    }

    public void generatePieceMoves(Consumer<Move> moves, int index, boolean attacksOnly) {
        final ChessPiece piece = board[index];
        if (piece.isKing()) {
            generateKingMoves(moves, piece, attacksOnly);
            return;
        }

        final PieceType pinType = !isChecked ? getPinType(piece) : PieceType.EMPTY;

		switch (piece.getType()) {
			case PAWN: 
				generatePawnMoves(moves, piece, pinType, attacksOnly);
				break;
			case KNIGHT: 
				generateKnightMoves(moves, piece, pinType, attacksOnly);
				break;
            case BISHOP:
                generateBishopMoves(moves, piece, pinType, attacksOnly);
                break;
            case ROOK:
                generateRookMoves(moves, piece, pinType, attacksOnly);
                break;
            case QUEEN:
                generateQueenMoves(moves, piece, pinType, attacksOnly);
                break;
            default:
                throw new IllegalArgumentException("Illegal empty square fed to function.");
		}
    }

    private void generatePawnMoves(Consumer<Move> moves, ChessPiece piece, PieceType pinType, boolean attacksOnly) {
        final long pawnBitboard = generatePawnBitboard(piece.getColor(), piece.getPos(), attacksOnly);
        addMoves(moves, piece, pinType, pawnBitboard);
        if (enPassantSquare != EMPTY) {
            final long pawnAttacks = PAWN_ATTACKS[piece.getColor().arrayIndex][piece.getPos()];
            final int enPassantMove = enPassantSquareToMove(piece.getColor(), enPassantSquare);
            if ((pawnAttacks & squareToBitboard(enPassantMove)) != 0) {
                final Move move = new Move(piece.getPos(), enPassantMove, true);
                if (isLegalMove(move, piece.getColor(), pinType, true)) moves.accept(move);
            }
        }
    }

    private void generateKnightMoves(Consumer<Move> moves, ChessPiece piece, PieceType pinType, boolean attacksOnly) {
        final long knightBitboard = generateKnightBitboard(piece.getColor(), piece.getPos(), attacksOnly);
        addMoves(moves, piece, pinType, knightBitboard);
    }

    private void generateBishopMoves(Consumer<Move> moves, ChessPiece piece, PieceType pinType, boolean attacksOnly) {
        final long bishopBitboard = generateBishopBitboard(piece.getColor(), piece.getPos(), attacksOnly);
        addMoves(moves, piece, pinType, bishopBitboard);
    }

    private void generateRookMoves(Consumer<Move> moves, ChessPiece piece, PieceType pinType, boolean attacksOnly) {
        final long rookBitboard = generateRookBitboard(piece.getColor(), piece.getPos(), attacksOnly);
        addMoves(moves, piece, pinType, rookBitboard);
    }

    private void generateQueenMoves(Consumer<Move> moves, ChessPiece piece, PieceType pinType, boolean attacksOnly) {
        final long queenBitboard = generateQueenBitboard(piece.getColor(), piece.getPos(), attacksOnly);
        addMoves(moves, piece, pinType, queenBitboard);
    }

    private void generateKingMoves(Consumer<Move> moves, ChessPiece piece, boolean attacksOnly) {
        final long kingBitboard = generateKingBitboard(piece.getColor(), piece.getPos(), attacksOnly);
        applyFunctionByBitIndices(kingBitboard, (int index) -> {
            final Move move = new Move(piece.getPos(), index);
            if (isLegalKingMove(move, piece.getColor())) moves.accept(move);
        });
        if (canCastle(piece, QUEENSIDE)) moves.accept(new Move(piece.getPos(), KING_CASTLE_MOVES[piece.getColor().arrayIndex][QUEENSIDE], true));
        if (canCastle(piece, KINGSIDE)) moves.accept(new Move(piece.getPos(), KING_CASTLE_MOVES[piece.getColor().arrayIndex][KINGSIDE], true));
    }

    public long generatePawnBitboard(PieceColor color, int square, boolean attacksOnly) {
        final long validAttackSquares = ALL_PIECES[flipColor(color).arrayIndex];
        final long pawnAttacks = PAWN_ATTACKS[color.arrayIndex][square] & validAttackSquares;
        if (attacksOnly) return pawnAttacks;
        long pawnMoves = PAWN_MOVES[color.arrayIndex][square] & ~OCCUPIED;
        if (getRow(square) == PAWN_STARTING_ROW[color.arrayIndex]) {
            final int numMoves = Long.bitCount(pawnMoves);
            if (numMoves == 1) {
                pawnMoves &= ~(color == PieceColor.WHITE ? RANK5 : RANK4);
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
        // final long kingMoves = filterIllegalKingMoves(color, possibleKingMoves & viableSquares);
        // return kingMoves;
        return possibleKingMoves & viableSquares;
    }

    private void addMoves(Consumer<Move> moves, ChessPiece piece, PieceType pinType, long bitboard) {
        applyFunctionByBitIndices(bitboard, (int index) -> addMove(moves, new Move(piece.getPos(), index), piece.getColor(), pinType));
    }

    private void addMove(Consumer<Move> moves, Move move, PieceColor color, PieceType pinType) {
        if (isLegalMove(move, color, pinType, isChecked)) moves.accept(move);
    }

    private boolean canCastle(ChessPiece piece, int side) {
        if (!castlingRights[piece.getColor().arrayIndex][side]) return false;

        final ChessPiece castlingRook = board[ROOK_POSITIONS[piece.getColor().arrayIndex][side]];
        if (!castlingRook.isRook() || castlingRook.getColor() != piece.getColor()) return false;

        if ((KING_CASTLE_EMPTY_SQUARES[piece.getColor().arrayIndex][side] & OCCUPIED) != 0) return false;

        final long attackBitboard = KING_CASTLE_ATTACK_SQUARES[piece.getColor().arrayIndex][side];
        if (isAttacked(piece.getColor(), OCCUPIED, attackBitboard)) return false;

        // if ((KING_CASTLE_ATTACK_SQUARES[piece.getColor().arrayIndex][side] & ATTACKS[flipColor(piece.getColor()).arrayIndex]) != 0) return false;

        return true;
    }

    private boolean isLegalKingMove(Move move, PieceColor color) {
        final long occupiedBitboard = setBit(clearBit(OCCUPIED, move.getStart()), move.getFinish());
        final long[] updatedAttackingPieces = PIECES[flipColor(color).arrayIndex].clone();
        for (final PieceType type : NON_KING_PIECES) {
            final long currentBitboard = updatedAttackingPieces[type.arrayIndex];
            updatedAttackingPieces[type.arrayIndex] = clearBit(currentBitboard, move.getFinish());
        }
        return !BitboardHelper.isAttacked(color, updatedAttackingPieces, occupiedBitboard, squareToBitboard(move.getFinish()));
    }

    private boolean isLegalMove(Move move, PieceColor color, PieceType pinType, boolean isChecked) {
        if (!isChecked && pinType == PieceType.EMPTY) return true;
        final long kingBitboard = PIECES[color.arrayIndex][PieceType.KING.arrayIndex];
        long occupiedBitboard = setBit(clearBit(OCCUPIED, move.getStart()), move.getFinish());
        if (move.isSpecial()) occupiedBitboard = clearBit(occupiedBitboard, enPassantSquare);

        final long[] attackingPieces = PIECES[flipColor(color).arrayIndex];

        if (isChecked) {
            final long[] updatedAttackingPieces = attackingPieces.clone();
            for (final PieceType type : NON_KING_PIECES) {
                long newBitboard = clearBit(updatedAttackingPieces[type.arrayIndex], move.getFinish());
                if (move.isSpecial()) newBitboard = clearBit(occupiedBitboard, enPassantSquare);
                updatedAttackingPieces[type.arrayIndex] &= newBitboard;
            }
            return !BitboardHelper.isAttacked(color, updatedAttackingPieces, occupiedBitboard, kingBitboard);
        }
        final long pinningPieces = clearBit(attackingPieces[pinType.arrayIndex] | attackingPieces[PieceType.QUEEN.arrayIndex], move.getFinish());
        return !BitboardHelper.sacrificesKing(pinType, pinningPieces, occupiedBitboard, kingBitboard);
    }

    private PieceType getPinType(ChessPiece piece) {
        final long kingBitboard = PIECES[piece.getColor().arrayIndex][PieceType.KING.arrayIndex];

        final long occupiedBitboard = clearBit(OCCUPIED, piece.getPos());

        return getAttackerType(piece.getColor(), occupiedBitboard, kingBitboard);
    }

    private boolean isAttacked(PieceColor color, long occupiedBitboard, long validSquares) {
        return getAttackerType(color, occupiedBitboard, validSquares) != PieceType.EMPTY;
    }

    private PieceType getAttackerType(PieceColor color, long occupiedBitboard, long validSquares) {
        final PieceColor opposingColor = flipColor(color);
        final long[] opposingPieces = PIECES[opposingColor.arrayIndex];
        return BitboardHelper.getAttackerType(color, opposingPieces, occupiedBitboard, validSquares);
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
        final long pawns = PIECES[color.arrayIndex][PieceType.PAWN.arrayIndex];
        final long opposingPieces = ALL_PIECES[flipColor(color).arrayIndex];
        final long pawnBitboardOneSquare = generatePawnBitboardOneSquare(color);
        final long pawnBitboardTwoSquares = generatePawnBitboardTwoSquares(color, pawnBitboardOneSquare);
        final long pawnBitboardAttacksLeft = generatePawnBitboardAttacksLeft(color, pawns, opposingPieces);
        final long pawnBitboardAttacksRight = generatePawnBitboardAttacksRight(color, pawns, opposingPieces);
        final long pawnBitboardEnPassant = generatePawnBitboardEnPassant(color);

        final int[] pawnOffsets = pawnIndexOffsets[color.arrayIndex];

        applyFunctionByBitIndices(pawnBitboardOneSquare, (int index) -> moves.add(new Move(index + pawnOffsets[0], index)));
        applyFunctionByBitIndices(pawnBitboardTwoSquares, (int index) -> moves.add(new Move(index + pawnOffsets[1], index)));
        applyFunctionByBitIndices(pawnBitboardAttacksLeft, (int index) -> moves.add(new Move(index + pawnOffsets[2], index)));
        applyFunctionByBitIndices(pawnBitboardAttacksRight, (int index) -> moves.add(new Move(index + pawnOffsets[3], index)));
        applyFunctionByBitIndices(pawnBitboardEnPassant, (int index) -> moves.add(new Move(index, enPassantSquare)));
    }

    private long generatePawnBitboardOneSquare(PieceColor color) {
        final long pawns = PIECES[color.arrayIndex][PieceType.PAWN.arrayIndex];
        if (color == PieceColor.WHITE) {
            return pawns >>> UP.absoluteArrayValue & ~OCCUPIED;
        }
        return pawns << DOWN.absoluteArrayValue & ~OCCUPIED;
    }

    private long generatePawnBitboardTwoSquares(PieceColor color, long pawnBitboardOneSquare) {
        final long pawns = PIECES[color.arrayIndex][PieceType.PAWN.arrayIndex];
        final long startingRow = RANKS[PAWN_STARTING_ROW[color.arrayIndex]];

        final long pawnsOnStartingRow = pawns & startingRow;

        final long pawnsThatCanMoveOneSquare = pawns & (color == PieceColor.WHITE ? pawnBitboardOneSquare << DOWN.absoluteArrayValue : pawnBitboardOneSquare >>> UP.absoluteArrayValue);
        final long potentialPawns = (pawnsThatCanMoveOneSquare & pawnsOnStartingRow);

        final long twoSpacesAhead = color == PieceColor.WHITE ? potentialPawns >>> UP.absoluteArrayValue * 2 : potentialPawns << DOWN.absoluteArrayValue * 2;
        return twoSpacesAhead & ~OCCUPIED;
    }


    private long generatePawnBitboardEnPassant(PieceColor color) {
        if (enPassantSquare == EMPTY) return 0L;
        final long epBitBoard = 1L << enPassantSquare;
        final long leftPawn = (epBitBoard & ~FILES[0]) >>> LEFT.absoluteArrayValue;
        final long rightPawn = (epBitBoard & ~FILES[7]) << RIGHT.absoluteArrayValue;
        final long pawns = PIECES[color.arrayIndex][PieceType.PAWN.arrayIndex];
        return (leftPawn | rightPawn) & pawns;
    }

    public void setPiece(int index, ChessPiece piece) {

        final boolean wasOccupied = isOccupied(index);

        if (wasOccupied) clearPiece(index);

        board[index] = piece;
        piece.setPos(index);

        PIECES[piece.getColor().arrayIndex][piece.getType().arrayIndex] = setBit(PIECES[piece.getColor().arrayIndex][piece.getType().arrayIndex], index);
        ALL_PIECES[piece.getColor().arrayIndex] = setBit(ALL_PIECES[piece.getColor().arrayIndex], index);
        OCCUPIED = setBit(OCCUPIED, index);
        pieces[piece.getColor().arrayIndex].add(piece);
    }

    public void clearPiece(int index) {

        final ChessPiece piece = board[index];

        PIECES[piece.getColor().arrayIndex][piece.getType().arrayIndex] = clearBit(PIECES[piece.getColor().arrayIndex][piece.getType().arrayIndex], index);
        ALL_PIECES[piece.getColor().arrayIndex] = clearBit(ALL_PIECES[piece.getColor().arrayIndex], index);
        OCCUPIED = clearBit(OCCUPIED, index);
        pieces[piece.getColor().arrayIndex].remove(piece);

        board[index] = ChessPiece.empty();
    }

    public void updateCheck(PieceColor color) {
        isChecked = isAttacked(color, OCCUPIED, PIECES[color.arrayIndex][PieceType.KING.arrayIndex]);
    }

    public void setCheck(boolean value) {
        isChecked = value;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public PieceSet getPieces(PieceColor color) {
        return pieces[color.arrayIndex];
    }

    public ChessPiece getPiece(int index) {
        return board[index];
    }

    public int getPieceCount(PieceColor color, PieceType type) {
        return Long.bitCount(PIECES[color.arrayIndex][type.arrayIndex]);
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
