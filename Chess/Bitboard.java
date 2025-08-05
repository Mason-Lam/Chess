package Chess;

import java.util.Arrays;
import java.util.Collection;
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

    private long OCCUPIED;

    private int enPassantSquare;

    private final boolean[][] castlingRights;

    private boolean isChecked;

    private final PieceType[] pins;

    public Bitboard(String fen) {
        PIECES = new long[2][6];
        ALL_PIECES = new long[2];
        board = new ChessPiece[64];
        OCCUPIED = 0L;

        enPassantSquare = EMPTY;
        castlingRights = new boolean[2][2]; //Black: Queenside, Kingside, White: Queenside, Kingside
        
        Arrays.fill(ALL_PIECES, 0L);
        for (int i = 0; i < 64; i ++) {
            board[i] = ChessPiece.empty();
        }

        pins = new PieceType[64];

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
                final ChessPiece piece = charToPiece(letter, pos);

                setPiece(pos, piece);

                pos++;
            }
            else break;
		}
    }

    public void generateAllMoves(Collection<Move> moves, PieceColor color, boolean attacksOnly) {
        generateAllMoves((Move move) -> moves.add(move), color, attacksOnly);
    }

    public void generateAllMoves(Consumer<Move> moves, PieceColor color, boolean attacksOnly) {
        generateAllPawnMoves(moves, color, attacksOnly);
        final long[] movingPieces = PIECES[color.arrayIndex];
        for (int index = 1; index < movingPieces.length; index++) {
            final long bitboard = movingPieces[index];
            if (bitboard == 0L) continue; //No pieces of this type.
            applyFunctionByBitIndices(bitboard, (int pieceIndex) -> generatePieceMoves(moves, pieceIndex, attacksOnly));
        }
    }

    public void generatePieceMoves(Collection<Move> moves, int index, boolean attacksOnly) {
        generatePieceMoves((Move move) -> moves.add(move), index, attacksOnly);
    }

    public void generatePieceMoves(Consumer<Move> moves, int index, boolean attacksOnly) {
        final ChessPiece piece = board[index];
        if (piece.isKing()) {
            generateKingMoves(moves, piece, attacksOnly);
            return;
        }

		switch (piece.getType()) {
			case PAWN: 
				generatePawnMoves(moves, piece, attacksOnly);
				break;
			case KNIGHT: 
				generateKnightMoves(moves, piece, attacksOnly);
				break;
            case BISHOP:
                generateBishopMoves(moves, piece, attacksOnly);
                break;
            case ROOK:
                generateRookMoves(moves, piece, attacksOnly);
                break;
            case QUEEN:
                generateQueenMoves(moves, piece, attacksOnly);
                break;
            default:
                throw new IllegalArgumentException("Illegal empty square fed to function.");
		}
    }

    private void generatePawnMoves(Consumer<Move> moves, ChessPiece piece, boolean attacksOnly) {
        final long validAttackSquares = ALL_PIECES[flipColor(piece.getColor()).arrayIndex];
        final long pawnBitboard = BitboardHelper.generatePawnBitboard(piece.getColor(), validAttackSquares, OCCUPIED, piece.getPos(), attacksOnly);
        addMoves(moves, piece, pawnBitboard);

        if (enPassantSquare != EMPTY) {
            final long pawnAttacks = PAWN_ATTACKS[piece.getColor().arrayIndex][piece.getPos()];
            final int enPassantMove = enPassantSquareToMove(piece.getColor(), enPassantSquare);
            if ((pawnAttacks & squareToBitboard(enPassantMove)) != 0) {
                final Move move = new Move(piece.getPos(), enPassantMove, true);
                if (isLegalMove(move, piece.getColor(), true)) moves.accept(move);
            }
        }
    }

    private void generateKnightMoves(Consumer<Move> moves, ChessPiece piece, boolean attacksOnly) {
        final long validSquares = ALL_PIECES[flipColor(piece.getColor()).arrayIndex] | (!attacksOnly ? ~OCCUPIED : 0L);
        final long knightBitboard = BitboardHelper.generateKnightBitboard(validSquares, piece.getPos());
        addMoves(moves, piece, knightBitboard);
    }

    private void generateBishopMoves(Consumer<Move> moves, ChessPiece piece, boolean attacksOnly) {
        final long validSquares = ALL_PIECES[flipColor(piece.getColor()).arrayIndex] | (!attacksOnly ? ~OCCUPIED : 0L);
        final long bishopBitboard = BitboardHelper.generateBishopBitboard(validSquares, OCCUPIED, piece.getPos());
        addMoves(moves, piece, bishopBitboard);
    }

    private void generateRookMoves(Consumer<Move> moves, ChessPiece piece, boolean attacksOnly) {
        final long validSquares = ALL_PIECES[flipColor(piece.getColor()).arrayIndex] | (!attacksOnly ? ~OCCUPIED : 0L);
        final long rookBitboard = BitboardHelper.generateRookBitboard(validSquares, OCCUPIED, piece.getPos());
        addMoves(moves, piece, rookBitboard);
    }

    private void generateQueenMoves(Consumer<Move> moves, ChessPiece piece, boolean attacksOnly) {
        final long validSquares = ALL_PIECES[flipColor(piece.getColor()).arrayIndex] | (!attacksOnly ? ~OCCUPIED : 0L);
        final long queenBitboard = BitboardHelper.generateBishopBitboard(validSquares, OCCUPIED, piece.getPos()) | BitboardHelper.generateRookBitboard(validSquares, OCCUPIED, piece.getPos());
        addMoves(moves, piece, queenBitboard);
    }

    private void generateKingMoves(Consumer<Move> moves, ChessPiece piece, boolean attacksOnly) {
        final long validSquares = ALL_PIECES[flipColor(piece.getColor()).arrayIndex] | (!attacksOnly ? ~OCCUPIED : 0L);
        final long kingBitboard = BitboardHelper.generateKingBitboard(validSquares, piece.getPos());
        applyFunctionByBitIndices(kingBitboard, (int index) -> {
            final Move move = new Move(piece.getPos(), index);
            if (isLegalKingMove(move, piece.getColor())) moves.accept(move);
        });
        if (attacksOnly) return;
        if (canCastle(piece, QUEENSIDE)) moves.accept(new Move(piece.getPos(), KING_CASTLE_MOVES[piece.getColor().arrayIndex][QUEENSIDE], true));
        if (canCastle(piece, KINGSIDE)) moves.accept(new Move(piece.getPos(), KING_CASTLE_MOVES[piece.getColor().arrayIndex][KINGSIDE], true));
    }

    private void addMoves(Consumer<Move> moves, ChessPiece piece, long bitboard) {
        applyFunctionByBitIndices(bitboard, (int index) -> addMove(moves, new Move(piece.getPos(), index), piece.getColor()));
    }

    private void addMove(Consumer<Move> moves, Move move, PieceColor color) {
        if (isLegalMove(move, color, isChecked)) moves.accept(move);
    }

    private boolean canCastle(ChessPiece piece, int side) {
        if (!castlingRights[piece.getColor().arrayIndex][side]) return false;

        final ChessPiece castlingRook = board[ROOK_POSITIONS[piece.getColor().arrayIndex][side]];
        if (!castlingRook.isRook() || castlingRook.getColor() != piece.getColor()) return false;

        if ((KING_CASTLE_EMPTY_SQUARES[piece.getColor().arrayIndex][side] & OCCUPIED) != 0) return false;

        final long attackBitboard = KING_CASTLE_ATTACK_SQUARES[piece.getColor().arrayIndex][side];
        if (isAttacked(piece.getColor(), OCCUPIED, attackBitboard)) return false;

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

    private boolean isLegalMove(Move move, PieceColor color, boolean isChecked) {
        PieceType pinType = pins[move.getStart()];
        if (!isChecked) {
            if (pinType == null) {
                pinType = getPinType(board[move.getStart()]);
                pins[move.getStart()] = pinType;
            }
            if (pinType == PieceType.EMPTY) return true;
        }
        
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
        final long[] opposingPieces = PIECES[flipColor(piece.getColor()).arrayIndex];

        return BitboardHelper.getPinType(opposingPieces, occupiedBitboard, kingBitboard);
    }

    private boolean isAttacked(PieceColor color, long occupiedBitboard, long validSquares) {
        return getAttackerType(color, occupiedBitboard, validSquares) != PieceType.EMPTY;
    }

    private PieceType getAttackerType(PieceColor color, long occupiedBitboard, long validSquares) {
        final PieceColor opposingColor = flipColor(color);
        final long[] opposingPieces = PIECES[opposingColor.arrayIndex];
        return BitboardHelper.getAttackerType(color, opposingPieces, occupiedBitboard, validSquares);
    }

    private static int[][] pawnIndexOffsets = {
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

    public void generateAllPawnMoves(Collection<Move> moves, PieceColor color, boolean attacksOnly) {
        generateAllPawnMoves((Move move) -> moves.add(move), color, attacksOnly);
    }

    public void generateAllPawnMoves(Consumer<Move> moves, PieceColor color, boolean attacksOnly) {
        final long pawns = PIECES[color.arrayIndex][PieceType.PAWN.arrayIndex];
        final long opposingPieces = ALL_PIECES[flipColor(color).arrayIndex];
        final int[] pawnOffsets = pawnIndexOffsets[color.arrayIndex];

        final long pawnBitboardAttacksLeft = generatePawnBitboardAttacksLeft(color, pawns, opposingPieces);
        final long pawnBitboardAttacksRight = generatePawnBitboardAttacksRight(color, pawns, opposingPieces);
        final long pawnBitboardEnPassant = generatePawnBitboardEnPassant(color, pawns, enPassantSquare);

        applyFunctionByBitIndices(pawnBitboardAttacksLeft, (int index) -> addMove(moves, new Move(index + pawnOffsets[2], index), color));
        applyFunctionByBitIndices(pawnBitboardAttacksRight, (int index) -> addMove(moves, new Move(index + pawnOffsets[3], index), color));
        applyFunctionByBitIndices(pawnBitboardEnPassant, (int index) -> {
            final int enPassantMove = enPassantSquareToMove(color, enPassantSquare);
            final Move move = new Move(index, enPassantMove, true);
            if (isLegalMove(move, color, true)) moves.accept(move);
        });

        if (attacksOnly) return;

        final long pawnBitboardOneSquare = generatePawnBitboardOneSquare(color, pawns, OCCUPIED);
        final long pawnBitboardTwoSquares = generatePawnBitboardTwoSquares(color, pawnBitboardOneSquare, OCCUPIED);

        applyFunctionByBitIndices(pawnBitboardOneSquare, (int index) -> addMove(moves, new Move(index + pawnOffsets[0], index), color));
        applyFunctionByBitIndices(pawnBitboardTwoSquares, (int index) -> addMove(moves, new Move(index + pawnOffsets[1], index), color));
    }

    public void setPiece(int index, ChessPiece piece) {

        final boolean wasOccupied = isOccupied(index);

        if (wasOccupied) clearPiece(index);

        board[index] = piece;
        piece.setPos(index);

        PIECES[piece.getColor().arrayIndex][piece.getType().arrayIndex] = setBit(PIECES[piece.getColor().arrayIndex][piece.getType().arrayIndex], index);
        ALL_PIECES[piece.getColor().arrayIndex] = setBit(ALL_PIECES[piece.getColor().arrayIndex], index);
        OCCUPIED = setBit(OCCUPIED, index);
    }

    public void clearPiece(int index) {

        final ChessPiece piece = board[index];

        PIECES[piece.getColor().arrayIndex][piece.getType().arrayIndex] = clearBit(PIECES[piece.getColor().arrayIndex][piece.getType().arrayIndex], index);
        ALL_PIECES[piece.getColor().arrayIndex] = clearBit(ALL_PIECES[piece.getColor().arrayIndex], index);
        OCCUPIED = clearBit(OCCUPIED, index);

        board[index] = ChessPiece.empty();
    }

    public void resetPins() {
        Arrays.fill(pins, null);
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
