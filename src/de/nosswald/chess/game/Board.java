package de.nosswald.chess.game;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import de.nosswald.chess.Chess;
import de.nosswald.chess.game.ai.OpponentPlayer;
import de.nosswald.chess.game.piece.Piece;
import de.nosswald.chess.game.piece.impl.*;
import de.nosswald.chess.gui.screen.impl.GameResultScreen;
import de.nosswald.chess.logger.LoggerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Nils Osswald
 * @author Noah Gerber
 */
public final class Board
{
    /**
     * Contains every {@link Piece} on the {@link Board}
     */
    private final List<Piece> pieces = new ArrayList<>();

    /**
     * Contains every {@link Move} that has been done on the yet
     */
    private final List<Move> history = new ArrayList<>();

    /**
     * Stores the {@link Side} which moves next
     */
    private Side nextMove = Side.WHITE;

    /**
     * Stores the {@link Side} which is being controlled by the player
     */
    private final Side playerSide = Math.random() > .5 ? Side.WHITE : Side.BLACK;

    /**
     * Stores the {@link OpponentPlayer}
     */
    private final OpponentPlayer opponentPlayer = new OpponentPlayer(playerSide.flip());

    /**
     * Stores the currently selected {@link Piece} (<code>null</code> if nothing is selected)
     */
    @Nullable
    private Piece selected;

    private boolean gameOver;
    private boolean legitimacyChecking = true;

    /**
     * Initializes every {@link Piece} on the {@link Board}
     */
    public void initialize()
    {
        IntStream.range(0, 16).forEach(i -> {
            boolean b = i % 2 == 0;

            pieces.add(new Pawn(b ? Side.BLACK : Side.WHITE, new Position(i / 2, b ? 1 : 6)));

            if (i < 4)
            {
                pieces.add(new Rook(b ? Side.BLACK : Side.WHITE, new Position(i / 2 == 0 ? 0 : 7, b ? 0 : 7)));
                pieces.add(new Knight(b ? Side.BLACK : Side.WHITE, new Position(i / 2 == 0 ? 1 : 6, b ? 0 : 7)));
                pieces.add(new Bishop(b ? Side.BLACK : Side.WHITE, new Position(i / 2 == 0 ? 2 : 5, b ? 0 : 7)));
            }
            if (i < 2)
            {
                pieces.add(new Queen(b ? Side.BLACK : Side.WHITE, new Position(3, b ? 0 : 7)));
                pieces.add(new King(b ? Side.BLACK : Side.WHITE, new Position(4, b ? 0 : 7)));
            }
        });

        Chess.getInstance().getLogger().print(LoggerLevel.INFO, "Added all pieces to the board");
        Chess.getInstance().getLogger().printFormat(LoggerLevel.INFO, "Player moves %s",
                playerSide.toString().toLowerCase());

        // in case the opponent has the first move
        if (nextMove != playerSide)
            opponentPlayer.awaitResponse();
    }

    /**
     * Handles the {@link java.awt.event.MouseEvent} based on the given {@link Position}
     *
     * @param position the clicked {@link Position}
     */
    public void onClick(Position position)
    {
        if (nextMove != playerSide || gameOver)
            return;

        if (selected == null)
        {
            // select piece
            Piece clicked = getPiece(position);

            if (clicked != null && clicked.getSide() == nextMove)
            {
                selected = clicked;
                Chess.getInstance().getLogger().printFormat(LoggerLevel.DEBUG,
                        "Selected %s on %s [%d possible moves]", clicked.getClass().getSimpleName(),
                        position.toString(), clicked.getPossibleMoves().size()
                );
            }
        }
        else
        {
            // move selected piece to the clicked column and row
            selected.getPossibleMoves().stream()
                    .filter(m -> m.getTo().equals(position))
                    .findFirst().ifPresent(m ->
                    {
                        Chess.getInstance().getLogger().printFormat(LoggerLevel.DEBUG,
                                "Player moved %s from %s to %s", selected.getClass().getSimpleName(),
                                selected.getPosition().toString(), m.getTo().toString());
                        makeMove(new Move(selected.getPosition(), position));

                        // opponents move
                        opponentPlayer.awaitResponse();
                    }
            );

            // unselect current piece
            selected = null;
        }
    }

    /**
     * Performs the given {@link Move} on the {@link Board}
     *
     * @param move the {@link Move}
     */
    public void makeMove(@NotNull Move move)
    {
        history.add(new Move(move.getFrom(), move.getTo()));
        getPiece(move.getFrom()).setPosition(move.getTo());

        // flip sides
        nextMove = nextMove.flip();
        Chess.getInstance().getLogger().printFormat(LoggerLevel.DEBUG,
                "%s's turn", nextMove.toString());

        // check if game is over
        if (isStaleMate() || isCheckMate(nextMove))
        {
            gameOver = true;
            selected = null;

            if (isStaleMate())
            {
                nextMove = null; // to identify that the match has ended in a draw
                Chess.getInstance().getLogger().print(LoggerLevel.INFO, "The match has ended in a draw");
            }
            else
                Chess.getInstance().getLogger().printFormat(LoggerLevel.INFO,
                        "%s has won the match", nextMove.flip().toString());

            Chess.getInstance().getFrame().setScreen(new GameResultScreen(this));
        }
    }

    /**
     * @param side the {@link Side} to be checked
     * @return Whether the {@link Side}'s {@link King} is in check or not
     */
    public boolean isInCheck(Side side)
    {
        List<Piece> piecesClone = new ArrayList<>(pieces);
        List<Piece> anotherPiecesClone = new ArrayList<>(pieces);

        return piecesClone.stream()
                .filter(p -> p instanceof King)
                .anyMatch(k -> anotherPiecesClone.stream()
                        .filter(p -> p.getSide() != side)
                        .anyMatch(p -> p.getPossibleMoves().stream()
                                .anyMatch(m -> k.getPosition().equals(m.getTo()))));
    }

    /**
     * @param side the {@link Side} to be checked
     * @return Whether it's checkmate for the given {@link Side} or not
     */
    public boolean isCheckMate(Side side)
    {
        List<Piece> piecesClone = new ArrayList<>(pieces);

        return piecesClone.stream()
                .filter(p -> p.getSide() == side)
                .allMatch(p -> p.getPossibleMoves().isEmpty()) && isInCheck(side);
    }

    /**
     * @return If both sides are unable to move
     */
    private boolean isStaleMate()
    {
        List<Piece> piecesClone = new ArrayList<>(pieces);

        return piecesClone.stream()
                .filter(p -> p.getSide() == nextMove)
                .allMatch(p -> p.getPossibleMoves().isEmpty()) && !isInCheck(nextMove);
    }

    /**
     * @param position the {@link Position}
     * @return The {@link Piece} on the given {@link Position} (<code>null</code> if the field is empty)
     */
    @Nullable
    public Piece getPiece(Position position)
    {
        return pieces.stream().filter(p -> p.getPosition().equals(position)).findFirst().orElse(null);
    }

    /**
     * @param position the {@link Position}
     * @return Whether the given {@link Position} has a {@link Piece} or not
     */
    public boolean hasPiece(Position position)
    {
        return pieces.stream().anyMatch(p -> p.getPosition().equals(position));
    }

    /**
     * @return A {@link List} of all {@link Piece}'s on the {@link Board}
     */
    public List<Piece> getPieces()
    {
        return pieces;
    }

    /**
     * @return Every {@link Move} that has been done yet
     */
    public List<Move> getHistory()
    {
        return history;
    }

    /**
     * @return The {@link Side} which moves next
     */
    public Side getNextMove()
    {
        return nextMove;
    }

    /**
     * @return The currently selected {@link Piece} (<code>null</code> if nothing is selected)
     */
    public Piece getSelected()
    {
        return selected;
    }

    public void setLegitimacyChecking(boolean legitimacyChecking)
    {
        this.legitimacyChecking = legitimacyChecking;
    }

    public boolean isLegitimacyChecking()
    {
        return legitimacyChecking;
    }
}
