package com.sysbot32.whistler.solitaire.model;

import com.sysbot32.whistler.card.Card;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A card sitting in a tableau pile, with face-up / face-down as board state
 * (not a UI-only flag).
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public final class TableauCard {
    private final Card card;
    private final boolean faceUp;

    public TableauCard faceUp() {
        return this.faceUp ? this : new TableauCard(this.card, true);
    }
}
