package com.repp.aquaticremineztools.data.model

enum class MarketCategory(val id: String, val displayName: String) {
    OVERVIEW("overview", "Overview"),
    INDICES("indices", "Indices"),
    STOCKS("stocks", "Stocks"),
    FOREX("forex", "Forex"),
    CRYPTO("crypto", "Crypto"),
    COMMODITIES("commodities", "Commodities"),
    ECONOMICS("economics", "Economics"),
    ETFS("etfs", "ETFs & Sectors"),
    WATCHLIST("watchlist", "Watchlist"),
    ALERTS("alerts", "Alerts"),
    PORTFOLIO("portfolio", "Portfolio")
}
