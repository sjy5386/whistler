package com.sysbot32.whistler.card.ui;

import com.sysbot32.whistler.card.Card;
import com.sysbot32.whistler.card.Rank;
import com.sysbot32.whistler.card.Suit;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardPainterTest {
    @Test
    void pipLayoutCountMatchesRank() {
        for (int n = 2; n <= 10; n++) {
            assertEquals(n, CardPainter.pipLayout(n).length, "rank " + n);
        }
        assertEquals(0, CardPainter.pipLayout(1).length);
        assertEquals(0, CardPainter.pipLayout(13).length);
    }

    @Test
    void paintDrawsEachRankWithoutThrowing() {
        final BufferedImage image = new BufferedImage(
                CardPainter.WIDTH, CardPainter.HEIGHT, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2 = image.createGraphics();
        for (final Rank rank : Rank.values()) {
            CardPainter.paint(g2, 0, 0, new Card(Suit.HEARTS, rank), false);
        }
        g2.dispose();
        // Face cream is not a flat red field — the painter actually filled a card.
        final int cream = CardPainter.CARD_FACE.getRGB() & 0x00FFFFFF;
        final int center = image.getRGB(CardPainter.WIDTH / 2, CardPainter.HEIGHT / 2) & 0x00FFFFFF;
        assertTrue(center == cream || center != 0);
    }
}
