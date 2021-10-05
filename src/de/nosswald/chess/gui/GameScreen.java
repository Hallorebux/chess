package de.nosswald.chess.gui;

import de.nosswald.chess.Chess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Noah Gerber
 * @author Nils Osswald
 */
public class GameScreen extends JPanel
{
    public GameScreen()
    {
        this.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                Chess.getInstance().getBoard().onClick(e.getX() / (getHeight() / 8), e.getY() / (getHeight() / 8));
            }
        });
    }

    @Override
    public void paint(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D)g;
        //g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .25F));
        Chess.getInstance().getBoard().paint(g2d, getHeight());

        repaint();
    }
}
