package com.pengrad.telegrambot.model.request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * stas
 * 8/4/15.
 */
public class InlineKeyboardMarkup extends Keyboard implements Serializable {
    private final static long serialVersionUID = 0L;

    private final List<List<InlineKeyboardButton>> inline_keyboard;

    public InlineKeyboardMarkup() {
        this.inline_keyboard = new ArrayList<>();
    }

    @Deprecated
    public InlineKeyboardMarkup(InlineKeyboardButton[]... keyboard) {
        this.inline_keyboard = new ArrayList<>(keyboard==null ? 0 : keyboard.length);
        if(keyboard!=null) {
            for (InlineKeyboardButton[] line : keyboard) {
                this.inline_keyboard.add(Arrays.asList(line));
            }
        }
    }

    public static InlineKeyboardMarkup create(InlineKeyboardButton... keyboard) {
        return new InlineKeyboardMarkup().addLine(keyboard);
    }

    public InlineKeyboardMarkup addLine(InlineKeyboardButton... keyboard) {
        this.inline_keyboard.add(new ArrayList<>(Arrays.asList(keyboard)));
        return this;
    }

    public InlineKeyboardButton[][] inlineKeyboard() {
        InlineKeyboardButton[][] res = new InlineKeyboardButton[inline_keyboard.size()][];
        for (int i = 0; i < inline_keyboard.size(); i++) {
            List<InlineKeyboardButton> line = inline_keyboard.get(i);
            res[i] = line.toArray(new InlineKeyboardButton[]{});
        }
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InlineKeyboardMarkup)) return false;
        InlineKeyboardMarkup that = (InlineKeyboardMarkup) o;
        return Objects.equals(inline_keyboard, that.inline_keyboard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inline_keyboard);
    }
}
