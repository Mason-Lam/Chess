package Chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static Chess.Constants.DirectionConstants.Direction.*;
import static Chess.Constants.PieceConstants.PIECE_COLORS;
import static Chess.BitboardHelper.*;

import Chess.Constants.PieceConstants.PieceColor;
import Chess.Constants.PieceConstants.PieceType;

import static Chess.Constants.PositionConstants.*;
import static Chess.BoardUtil.*;

public class Bitboard {

    public final long[][] PIECES;

    public final long[] ALL_PIECES;

    private final ChessPiece[] board;

    private final PieceSet[] pieces;

    private final int[] kingPos;

    private long OCCUPIED;

    public final long[] ATTACKS;

    public final PieceSet[][] allAttackers;

    private int enPassantSquare;

    private final boolean[][] castlingRights;

    private ChessPiece pinningPiece;

    public Bitboard(String fen) {
        PIECES = new long[2][6];
        ALL_PIECES = new long[2];
        board = new ChessPiece[64];
        pieces = new PieceSet[2];
        pieces[0] = new PieceSet();
        pieces[1] = new PieceSet();
        kingPos = new int[2];
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

        pinningPiece = null;
        
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
            else break;
		}
    }

    public void generatePieceMoves(List<Move> moves, int index, boolean attacksOnly) {
        final ChessPiece piece = board[index];
        if (!piece.isKing() && kingInDoubleCheck(piece.getColor())) return; //If the king is double checked, then the king is the only piece that can move.

		pinningPiece = getPin(piece);

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
            case KING:
                generateKingMoves(moves, piece, attacksOnly);
                break;
            default:
                throw new IllegalArgumentException("Illegal empty square fed to function.");
		}
		pinningPiece = null;
    }

    private void generatePawnMoves(List<Move> moves, ChessPiece piece, boolean attacksOnly) {
        final long pawnBitboard = generatePawnBitboard(piece.getColor(), piece.getPos(), attacksOnly);
        addMoves(moves, piece, pawnBitboard);
        if (enPassantSquare != EMPTY) {
            final long pawnAttacks = PAWN_ATTACKS[piece.getColor().arrayIndex][piece.getPos()];
            final int enPassantMove = enPassantSquareToMove(piece.getColor(), enPassantSquare);
            if ((pawnAttacks & squareToBitboard(enPassantMove)) != 0) {
                final Move move = new Move(piece.getPos(), enPassantMove, true);
                if (isLegalMove(move)) moves.add(move);
            }
        }
    }

    private void generateKnightMoves(List<Move> moves, ChessPiece piece, boolean attacksOnly) {
        final long knightBitboard = generateKnightBitboard(piece.getColor(), piece.getPos(), attacksOnly);
        addMoves(moves, piece, knightBitboard);
    }

    private void generateBishopMoves(List<Move> moves, ChessPiece piece, boolean attacksOnly) {
        final long bishopBitboard = generateBishopBitboard(piece.getColor(), piece.getPos(), attacksOnly);
        addMoves(moves, piece, bishopBitboard);
    }

    private void generateRookMoves(List<Move> moves, ChessPiece piece, boolean attacksOnly) {
        final long rookBitboard = generateRookBitboard(piece.getColor(), piece.getPos(), attacksOnly);
        addMoves(moves, piece, rookBitboard);
    }

    private void generateQueenMoves(List<Move> moves, ChessPiece piece, boolean attacksOnly) {
        final long queenBitboard = generateQueenBitboard(piece.getColor(), piece.getPos(), attacksOnly);
        addMoves(moves, piece, queenBitboard);
    }

    private void generateKingMoves(List<Move> moves, ChessPiece piece, boolean attacksOnly) {
        final long kingBitboard = generateKingBitboard(piece.getColor(), piece.getPos(), attacksOnly);
        addMoves(moves, piece, kingBitboard);
        if (canCastle(piece, QUEENSIDE)) moves.add(new Move(piece.getPos(), KING_CASTLE_MOVES[piece.getColor().arrayIndex][QUEENSIDE], true));
        if (canCastle(piece, KINGSIDE)) moves.add(new Move(piece.getPos(), KING_CASTLE_MOVES[piece.getColor().arrayIndex][KINGSIDE], true));
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
        final long kingMoves = filterIllegalKingMoves(color, possibleKingMoves & viableSquares);
        return kingMoves;
    }

    private void addMoves(List<Move> moves, ChessPiece piece, long bitboard) {
        applyFunctionByBitIndices(bitboard, (int index) -> addMove(moves, new Move(piece.getPos(), index)));
    }

    private void addMove(List<Move> moves, Move move) {
        if (isLegalMove(move)) moves.add(move);
    }

    private boolean canCastle(ChessPiece piece, int side) {
        if (!castlingRights[piece.getColor().arrayIndex][side]) return false;

        final ChessPiece castlingRook = board[ROOK_POSITIONS[piece.getColor().arrayIndex][side]];
        if (!castlingRook.isRook() || castlingRook.getColor() != piece.getColor()) return false;

        if ((KING_CASTLE_EMPTY_SQUARES[piece.getColor().arrayIndex][side] & OCCUPIED) != 0) return false;

        if ((KING_CASTLE_ATTACK_SQUARES[piece.getColor().arrayIndex][side] & ATTACKS[flipColor(piece.getColor()).arrayIndex]) != 0) return false;

        return true;
    }

    private long filterIllegalKingMoves(PieceColor color, long bitboard) {
        return bitboard & ~ATTACKS[flipColor(color).arrayIndex];
    }

    private boolean isLegalMove(Move move) {
        final ChessPiece piece = board[move.getStart()];
        final PieceColor color = piece.getColor();
        
		if (piece.getType() == PieceType.KING) return isLegalKingMove(color, move);	//Seperate case for king moves.

		if (kingInDoubleCheck(color)) return false;	//Only king can move in double check.

		//If the king is in check and the move does not stop the check, the move is illegal.
		if (kingInCheck(color) && !stopsCheck(color, move)) return false;

		return !sacrificesKing(color, move);		//Checks if the move would sacrifice the king.
	}

	private boolean isLegalKingMove(PieceColor color, Move move) {
		if (isBitSet(ATTACKS[flipColor(color).arrayIndex], move.getFinish())) return false;		//If the square is attacked, the king cannot move there.

		if (!kingInCheck(color)) return true;		//If the king is not in check, any square that is not attacked is legal.

        final PieceSet attackers = getAttackers(color, move.getStart());
		//Iterates over each attacking the king.
		for (final ChessPiece attacker : attackers) {
			if (move.getFinish() == attacker.getPos()) return true;	//If the king captures the attacking piece, it's legal.

			//Checks if the king would still be in check on the same diagonal.
			if (attacker.isDiagonalAttacker()) {
				if (onSameDiagonal(move.getStart(), move.getFinish(), attacker.getPos())) return false;
			}

			//Checks if the king would still be in check on the same line.
			if (attacker.isLineAttacker()) {
				if (onSameLine(move.getStart(), move.getFinish(), attacker.getPos())) return false;
			}
		}
		return true;
	}

    private boolean stopsCheck(PieceColor color, Move move) {
		final int kingSquare = getKingPos(color);
		final ChessPiece attacker = getAttackers(color, kingSquare).iterator().next();

		if (move.isSpecial() && enPassantSquare == attacker.getPos()) return true;	//Pawn captures enPassant to remove attacker.

		if (attacker.isPawn() || attacker.isKnight()) return move.getFinish() == attacker.getPos();	//If the king is attacked by a knight or pawn, they must be captured.

		if (move.getFinish() == attacker.getPos()) return true;	//If the attacking piece is captured, the king will no longer be in check.

		//If the king is checked by a bishop or queen on a diagonal, it must be blocked.
		if (onDiagonal(kingSquare, attacker.getPos())) {
			return blocksDiagonal(attacker.getPos(), kingSquare, move.getFinish());
		}
		//If the king is checked by a rook or queen along a line, it must be blocked.
		return blocksLine(attacker.getPos(), kingSquare, move.getFinish());
	}

    private boolean sacrificesKing(PieceColor color, Move move) {
		final int kingSquare = getKingPos(color);
		//Runs if the move is enPassant; if the pawn is pinned then the enPassant doesn't matter and use normal test case.
		if (pinningPiece.isEmpty() && move.isSpecial()) {
			//Check if the enPassant pawn potentially blocks an attack on the king.
			if ((onDiagonal(kingSquare, enPassantSquare) || onLine(kingSquare, enPassantSquare))) {
				final PieceSet attackers = getAttackers(color, enPassantSquare);
				//Iterate over all pieces attacking the enPassant pawn.
				for (final ChessPiece attacker : attackers) {
					if (attacker.isPawn() || attacker.isKnight() || attacker.isKing()) continue;

					// Check if the enPassant pawn is on the path between the attacker and king.
					if (blocksDiagonal(attacker.getPos(), kingSquare, enPassantSquare)) {
						// If there is a clear path between enPassant pawn and king, the move is illegal.
						return clearPath(enPassantSquare, kingSquare);
					}

					// Check if the enPassant pawn is on the path between the attacker and king.
					if (blocksLine(attacker.getPos(), kingSquare, move.getStart())) {
						// If there is a clear path between enPassant pawn and king, the move is illegal.
						return clearPath(move.getStart(), kingSquare);
					}
				}
			}
			//Check if a rook or queen blocks enPassant move.
			if (onLine(kingSquare, move.getStart())) {
				final PieceSet attackers = getAttackers(color, move.getStart());
				for (final ChessPiece piece : attackers) {
					if (piece.isPawn() || piece.isKnight() || piece.isBishop() || piece.isKing()) continue;

					// Check if the pawn is on the path between the attacker and king.
					if (blocksLine(piece.getPos(), kingSquare, move.getStart())) {
						// If there is a clear path between enPassant pawn and king, the move is illegal.
						return clearPath(enPassantSquare, kingSquare);
					}
				}
			}
			return false;
		}
		//Normal run.
		if (pinningPiece.isEmpty()) return false;	//If the piece isn't pinned it can't sacrifice the king.

		//If the piece is pinning the piece on a diagonal, the move is legal if the piece moves to the same diagonal.
		if (onDiagonal(move.getStart(), kingSquare)) return !onSameDiagonal(move.getFinish(), kingSquare, pinningPiece.getPos());

		//If the piece is pinning the piece along a line, the move is legal if the piece moves to the same line.
		return !onSameLine(move.getFinish(), kingSquare, pinningPiece.getPos());
	}

    private ChessPiece getPin(ChessPiece piece) {
		if (piece.isKing()) return null;
        if (pinningPiece != null) return pinningPiece;

		final int kingSquare = getKingPos(piece.getColor());

		//If not aligned with the king or not attacked, then the piece can't be pinned.
		if (!(onDiagonal(kingSquare, piece.getPos()) || onLine(kingSquare, piece.getPos())) || !isAttacked(piece)) return ChessPiece.empty();

		final PieceSet attackers = getAttackers(piece.getColor(), piece.getPos());
		//Iterate over every attacking piece.
		for (final ChessPiece attacker : attackers) {
			if (attacker.isPawn() || attacker.isKnight() || attacker.isKing()) continue;	//Can't be pinned by these pieces.
            

			if (blocksDiagonal(attacker.getPos(), kingSquare, piece.getPos()) || blocksLine(attacker.getPos(), kingSquare, piece.getPos())) {
				return clearPath(kingSquare, piece.getPos()) ? attacker : ChessPiece.empty();
			}
		}
		return ChessPiece.empty();
	}

    private boolean clearPath(int start, int end) {
        return clearPath(start, end, OCCUPIED);
    }

    private boolean clearPath(int start, int end, long occupiedBitboard) {
        long mask;
        long magicNumber;
        long[] possiblePaths;
        if (onDiagonal(start, end)) {
            mask = BISHOP_MASKS[start];
            magicNumber = BISHOP_MAGIC_NUMBERS[start];
            possiblePaths = BISHOP_MOVES[start];
        }
        else if (onLine(start, end)) {
            mask = ROOK_MASKS[start];
            magicNumber = ROOK_MAGIC_NUMBERS[start];
            possiblePaths = ROOK_MOVES[start];
        }
        else return false;

        final long blockers = (mask & occupiedBitboard);
        final long path = possiblePaths[(int) ((blockers * magicNumber) >>> (64 - Long.bitCount(mask)))];
        return (path & squareToBitboard(end)) != 0;
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

    private long generatePawnBitboardAttacksLeft(PieceColor color) {
        final long pawns = PIECES[color.arrayIndex][PieceType.PAWN.arrayIndex];
        final long filteredPawns = pawns & ~FILES[0]; // Exclude leftmost file to avoid overflow

        final long leftAttacks = color == PieceColor.WHITE ? filteredPawns >>> UPLEFT.absoluteArrayValue : filteredPawns << DOWNLEFT.absoluteArrayValue;

        final long opposingPieces = ALL_PIECES[flipColor(color).arrayIndex];
        return leftAttacks & opposingPieces;
    }

    private long generatePawnBitboardAttacksRight(PieceColor color) {
        final long pawns = PIECES[color.arrayIndex][PieceType.PAWN.arrayIndex];
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
        final long pawns = PIECES[color.arrayIndex][PieceType.PAWN.arrayIndex];
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

        if (piece.isKing()) kingPos[piece.color.arrayIndex] = index;

        board[index] = piece;
        piece.setPos(index);

        updateAttacks(piece, false);

        PIECES[piece.getColor().arrayIndex][piece.getType().arrayIndex] = setBit(PIECES[piece.getColor().arrayIndex][piece.getType().arrayIndex], index);
        ALL_PIECES[piece.getColor().arrayIndex] = setBit(ALL_PIECES[piece.getColor().arrayIndex], index);
        OCCUPIED = setBit(OCCUPIED, index);
        pieces[piece.getColor().arrayIndex].add(piece);

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
        PIECES[piece.getColor().arrayIndex][piece.getType().arrayIndex] = clearBit(PIECES[piece.getColor().arrayIndex][piece.getType().arrayIndex], index);
        ALL_PIECES[piece.getColor().arrayIndex] = clearBit(ALL_PIECES[piece.getColor().arrayIndex], index);
        OCCUPIED = clearBit(OCCUPIED, index);
        pieces[piece.getColor().arrayIndex].remove(piece);

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

    public PieceSet getPieces(PieceColor color) {
        return pieces[color.arrayIndex];
    }

    public ChessPiece getPiece(int index) {
        return board[index];
    }

    public int getKingPos(PieceColor color) {
        return kingPos[color.arrayIndex];
    }

    public boolean isOccupied(int index) {
        return (OCCUPIED & (1L << index)) != 0;
    }

    public boolean isAttacked(ChessPiece piece) {
        return numAttacks(piece.getColor(), piece.getPos()) > 0;
    }

    public boolean kingInDoubleCheck(PieceColor color) {
        return numAttacks(color, getKingPos(color)) >= 2;
    }

    public boolean kingInCheck(PieceColor color) {
        return numAttacks(color, getKingPos(color)) != 0;
    }

    public int numAttacks(PieceColor color, int square) {
        return getAttackers(color, square).size();
    }

    public PieceSet getAttackers(PieceColor color, int square) {
        return allAttackers[flipColor(color).arrayIndex][square];
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
