/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.economy;

/**
 * The eleven economy operations, all carried by one request message so the channel does not grow per feature.
 */
public enum EconomyOperation
{
    REQUEST_DATA,
    BANK_DEPOSIT,
    BANK_WITHDRAW,
    BANK_TRANSFER,
    COUNTER_WITHDRAW,
    SHOP_BUY,
    EXCHANGE_PUBLISH_SELL,
    EXCHANGE_PUBLISH_BUY,
    EXCHANGE_ACCEPT,
    EXCHANGE_CANCEL,
    EXCHANGE_COLLECT,
    EXCHANGE_VIP,
    RECYCLE;

    public static EconomyOperation byOrdinal(int ordinal)
    {
        EconomyOperation[] values = values();

        if (ordinal < 0 || ordinal >= values.length)
        {
            return REQUEST_DATA;
        }

        return values[ordinal];
    }
}
