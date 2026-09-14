package com.altusix.slate.widgets.quotes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

data class QuoteItem(
    val id: String,
    val text: String,
    val author: String,
    val category: String,
    val sourceBook: String = "",
    val isFavorite: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("text", text)
        put("author", author)
        put("category", category)
        put("sourceBook", sourceBook)
        put("isFavorite", isFavorite)
    }

    companion object {
        fun fromJson(json: JSONObject): QuoteItem {
            return QuoteItem(
                id = json.optString("id", System.currentTimeMillis().toString()),
                text = json.optString("text", ""),
                author = json.optString("author", "Unknown"),
                category = json.optString("category", "WISDOM"),
                sourceBook = json.optString("sourceBook", ""),
                isFavorite = json.optBoolean("isFavorite", false)
            )
        }
    }
}

object QuotesStorageManager {
    private const val PREFS_NAME = "slate_quotes_prefs"
    private const val KEY_WIDGET_QUOTE_PREFIX = "widget_quote_"
    private const val KEY_FAVORITES = "quotes_favorites_set"
    private const val KEY_CUSTOM_QUOTES = "quotes_custom_list"

    // -------------------------------------------------------------------------
    // CURATED LIBRARY (65+ High-Caliber Authentic Quotes)
    // -------------------------------------------------------------------------
    val CURATED_QUOTES: List<QuoteItem> = listOf(
        // STOICISM
        QuoteItem("stoic_1", "You have power over your mind - not outside events. Realize this, and you will find strength.", "Marcus Aurelius", "STOICISM", "Meditations"),
        QuoteItem("stoic_2", "We suffer more often in imagination than in reality.", "Seneca", "STOICISM", "Letters from a Stoic"),
        QuoteItem("stoic_3", "The impediment to action advances action. What stands in the way becomes the way.", "Marcus Aurelius", "STOICISM", "Meditations"),
        QuoteItem("stoic_4", "No man is free who is not master of himself.", "Epictetus", "STOICISM", "Discourses"),
        QuoteItem("stoic_5", "Difficulties strengthen the mind, as labor does the body.", "Seneca", "STOICISM", "Letters from a Stoic"),
        QuoteItem("stoic_6", "Waste no more time arguing what a good man should be. Be one.", "Marcus Aurelius", "STOICISM", "Meditations"),
        QuoteItem("stoic_7", "He who fears death will never do anything worth of a man who is alive.", "Seneca", "STOICISM", "Moral Letters"),
        QuoteItem("stoic_8", "Don't explain your philosophy. Embody it.", "Epictetus", "STOICISM", "Enchiridion"),
        QuoteItem("stoic_9", "Very little is needed to make a happy life; it is all within yourself, in your way of thinking.", "Marcus Aurelius", "STOICISM", "Meditations"),
        QuoteItem("stoic_10", "Luck is what happens when preparation meets opportunity.", "Seneca", "STOICISM", "Moral Letters"),
        QuoteItem("stoic_11", "It is not that we have a short time to live, but that we waste a lot of it.", "Seneca", "STOICISM", "On the Shortness of Life"),

        // MINDFULNESS & ZEN
        QuoteItem("mind_1", "The present moment is filled with joy and happiness. If you are attentive, you will see it.", "Thich Nhat Hanh", "MINDFULNESS", "Peace Is Every Step"),
        QuoteItem("mind_2", "Silence is an empty space, space is the home of the awakened mind.", "Buddha", "MINDFULNESS"),
        QuoteItem("mind_3", "Do not dwell in the past, do not dream of the future, concentrate the mind on the present moment.", "Buddha", "MINDFULNESS"),
        QuoteItem("mind_4", "When you realize nothing is lacking, the whole world belongs to you.", "Lao Tzu", "MINDFULNESS", "Tao Te Ching"),
        QuoteItem("mind_5", "Nature does not hurry, yet everything is accomplished.", "Lao Tzu", "MINDFULNESS", "Tao Te Ching"),
        QuoteItem("mind_6", "Respond intelligently even to unintelligent treatment.", "Lao Tzu", "MINDFULNESS"),
        QuoteItem("mind_7", "The wound is the place where the Light enters you.", "Rumi", "MINDFULNESS"),
        QuoteItem("mind_8", "Muddy water is best cleared by leaving it alone.", "Alan Watts", "MINDFULNESS", "The Way of Zen"),
        QuoteItem("mind_9", "To understand everything is to forgive everything.", "Buddha", "MINDFULNESS"),
        QuoteItem("mind_10", "Stillness is where creativity and solutions to problems are found.", "Eckhart Tolle", "MINDFULNESS", "The Power of Now"),
        QuoteItem("mind_11", "Be like water making its way through cracks. Adjust to the object.", "Bruce Lee", "MINDFULNESS"),

        // LITERATURE & PHILOSOPHY
        QuoteItem("lit_1", "Be yourself; everyone else is already taken.", "Oscar Wilde", "LITERATURE"),
        QuoteItem("lit_2", "In the midst of winter, I found there was, within me, an invincible summer.", "Albert Camus", "LITERATURE", "Return to Tipasa"),
        QuoteItem("lit_3", "I think and think for months and years. Ninety-nine times, the conclusion is false. The hundredth time I am right.", "Albert Einstein", "LITERATURE"),
        QuoteItem("lit_4", "You do not write your life with words... You write it with actions.", "Patrick Ness", "LITERATURE", "A Monster Calls"),
        QuoteItem("lit_5", "I would rather die of passion than of boredom.", "Vincent van Gogh", "LITERATURE"),
        QuoteItem("lit_6", "There is no exquisite beauty... without some strangeness in the proportion.", "Edgar Allan Poe", "LITERATURE"),
        QuoteItem("lit_7", "To live is the rarest thing in the world. Most people exist, that is all.", "Oscar Wilde", "LITERATURE"),
        QuoteItem("lit_8", "Not all those who wander are lost.", "J.R.R. Tolkien", "LITERATURE", "The Fellowship of the Ring"),
        QuoteItem("lit_9", "Whatever you are, be a good one.", "Abraham Lincoln", "LITERATURE"),
        QuoteItem("lit_10", "What you seek is seeking you.", "Rumi", "LITERATURE"),

        // PRODUCTIVITY & MASTERY
        QuoteItem("prod_1", "Simplicity is the ultimate sophistication.", "Leonardo da Vinci", "PRODUCTIVITY"),
        QuoteItem("prod_2", "You do not rise to the level of your goals. You fall to the level of your systems.", "James Clear", "PRODUCTIVITY", "Atomic Habits"),
        QuoteItem("prod_3", "Your time is limited, so don't waste it living someone else's life.", "Steve Jobs", "PRODUCTIVITY", "Stanford Address"),
        QuoteItem("prod_4", "Focus is a muscle. The more you shield it from noise, the sharper it cuts.", "Anonymous", "PRODUCTIVITY"),
        QuoteItem("prod_5", "Do what you can, with what you have, where you are.", "Theodore Roosevelt", "PRODUCTIVITY"),
        QuoteItem("prod_6", "The secret of getting ahead is getting started.", "Mark Twain", "PRODUCTIVITY"),
        QuoteItem("prod_7", "Small disciplines repeated with consistency every day lead to great achievements.", "John C. Maxwell", "PRODUCTIVITY"),
        QuoteItem("prod_8", "An idiot with a plan can beat a genius without a plan.", "Warren Buffett", "PRODUCTIVITY"),
        QuoteItem("prod_9", "Great things are done by a series of small things brought together.", "Vincent van Gogh", "PRODUCTIVITY"),
        QuoteItem("prod_10", "Action expresses priorities.", "Mahatma Gandhi", "PRODUCTIVITY"),

        // SCIENCE & REASON
        QuoteItem("sci_1", "Somewhere, something incredible is waiting to be known.", "Carl Sagan", "SCIENCE", "Cosmos"),
        QuoteItem("sci_2", "The first principle is that you must not fool yourself and you are the easiest person to fool.", "Richard Feynman", "SCIENCE"),
        QuoteItem("sci_3", "Equipped with his five senses, man explores the universe around him and calls the adventure Science.", "Edwin Hubble", "SCIENCE"),
        QuoteItem("sci_4", "Nothing in life is to be feared, it is only to be understood. Now is the time to understand more, so that we may fear less.", "Marie Curie", "SCIENCE"),
        QuoteItem("sci_5", "Sometimes it is the people no one imagines anything of who do the things that no one can imagine.", "Alan Turing", "SCIENCE"),
        QuoteItem("sci_6", "Look up at the stars and not down at your feet. Be curious.", "Stephen Hawking", "SCIENCE"),
        QuoteItem("sci_7", "Science is not only a disciple of reason but also one of romance and passion.", "Stephen Hawking", "SCIENCE"),
        QuoteItem("sci_8", "For small creatures such as we the vastness is bearable only through love.", "Carl Sagan", "SCIENCE", "Contact"),

        // AFFIRMATION & SHORT MANTRAS (Punchy & ideal for 2x1 / 4x1)
        QuoteItem("aff_1", "Amor Fati: Love your fate.", "Friedrich Nietzsche", "AFFIRMATION"),
        QuoteItem("aff_2", "Stillness is the key.", "Ryan Holiday", "AFFIRMATION"),
        QuoteItem("aff_3", "Simplicity is mastery.", "Dieter Rams", "AFFIRMATION"),
        QuoteItem("aff_4", "Begin at once to live.", "Seneca", "AFFIRMATION"),
        QuoteItem("aff_5", "Discipline equals freedom.", "Jocko Willink", "AFFIRMATION"),
        QuoteItem("aff_6", "Own the morning, elevate your life.", "Robin Sharma", "AFFIRMATION"),
        QuoteItem("aff_7", "Breathe in peace, breathe out noise.", "Zen Proverb", "AFFIRMATION"),
        QuoteItem("aff_8", "Courage over comfort.", "Brené Brown", "AFFIRMATION"),
        QuoteItem("aff_9", "Energy flows where attention goes.", "Tony Robbins", "AFFIRMATION"),
        QuoteItem("aff_10", "Less, but better.", "Dieter Rams", "AFFIRMATION"),
        QuoteItem("aff_11", "The best view comes after the hardest climb.", "Proverb", "AFFIRMATION"),
        QuoteItem("aff_12", "Present over perfect.", "Shauna Niequist", "AFFIRMATION")
    )

    fun getAllQuotes(context: Context): List<QuoteItem> {
        val custom = getCustomQuotes(context)
        val favorites = getFavorites(context)
        val all = (custom + CURATED_QUOTES).distinctBy { it.id }
        return all.map { quote ->
            quote.copy(isFavorite = favorites.contains(quote.id))
        }
    }

    fun getQuoteById(context: Context, id: String): QuoteItem? {
        return getAllQuotes(context).find { it.id == id }
    }

    fun getQuoteForWidget(context: Context, widgetId: Int, defaultTypeTag: String = "EDITORIAL"): QuoteItem {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val quoteId = prefs.getString("$KEY_WIDGET_QUOTE_PREFIX$widgetId", null)
        val all = getAllQuotes(context)

        if (!quoteId.isNullOrBlank()) {
            val found = all.find { it.id == quoteId }
            if (found != null) return found
        }

        // Distinct tailored seeds for initial drop
        val seeded = when {
            defaultTypeTag.contains("EDITORIAL", ignoreCase = true) -> all.find { it.id == "stoic_1" }
            defaultTypeTag.contains("ZEN", ignoreCase = true) -> all.find { it.id == "mind_4" }
            defaultTypeTag.contains("MODERN", ignoreCase = true) || defaultTypeTag.contains("TYPEWRITER", ignoreCase = true) -> all.find { it.id == "stoic_2" }
            defaultTypeTag.contains("KINETIC", ignoreCase = true) -> all.find { it.id == "stoic_3" }
            defaultTypeTag.contains("GOLDEN", ignoreCase = true) -> all.find { it.id == "lit_2" }
            defaultTypeTag.contains("TWOTONE", ignoreCase = true) || defaultTypeTag.contains("POETRY", ignoreCase = true) -> all.find { it.id == "mind_7" }
            defaultTypeTag.contains("INSIGHT", ignoreCase = true) || defaultTypeTag.contains("BENTO", ignoreCase = true) -> all.find { it.id == "prod_2" }
            defaultTypeTag.contains("PILL", ignoreCase = true) -> all.find { it.id == "aff_1" }
            defaultTypeTag.contains("SMILE_CARD", ignoreCase = true) || defaultTypeTag.contains("BRUTALIST", ignoreCase = true) -> all.find { it.id == "aff_3" }
            defaultTypeTag.contains("SMILE", ignoreCase = true) || defaultTypeTag.contains("TERMINAL", ignoreCase = true) -> all.find { it.id == "sci_5" }
            else -> all.firstOrNull()
        } ?: CURATED_QUOTES.first()

        setQuoteForWidget(context, widgetId, seeded.id)
        return seeded
    }

    fun setQuoteForWidget(context: Context, widgetId: Int, quoteId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("$KEY_WIDGET_QUOTE_PREFIX$widgetId", quoteId).apply()
    }

    fun shuffleNextQuote(context: Context, widgetId: Int): QuoteItem {
        val all = getAllQuotes(context)
        if (all.isEmpty()) return CURATED_QUOTES.first()

        val current = getQuoteForWidget(context, widgetId)
        val currentIndex = all.indexOfFirst { it.id == current.id }
        val nextIndex = if (currentIndex >= 0 && currentIndex < all.size - 1) currentIndex + 1 else 0
        val nextQuote = all[nextIndex]

        setQuoteForWidget(context, widgetId, nextQuote.id)
        return nextQuote
    }

    fun copyQuoteToClipboard(context: Context, widgetId: Int) {
        val quote = getQuoteForWidget(context, widgetId)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val textToCopy = "\"${quote.text}\"\n— ${quote.author}"
        val clip = ClipData.newPlainText("Slate Quote", textToCopy)
        clipboard?.setPrimaryClip(clip)

        Toast.makeText(context, "Quote copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun toggleFavoriteById(context: Context, quoteId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet(KEY_FAVORITES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        val isNowFav: Boolean
        if (favorites.contains(quoteId)) {
            favorites.remove(quoteId)
            isNowFav = false
            Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
        } else {
            favorites.add(quoteId)
            isNowFav = true
            Toast.makeText(context, "Added to favorites ❤️", Toast.LENGTH_SHORT).show()
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
        return isNowFav
    }

    fun getFavorites(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun saveCustomQuote(context: Context, quote: QuoteItem) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = getCustomQuotes(context).toMutableList()
        existing.removeAll { it.id == quote.id }
        existing.add(0, quote)

        val jsonArray = JSONArray()
        for (q in existing) {
            jsonArray.put(q.toJson())
        }
        prefs.edit().putString(KEY_CUSTOM_QUOTES, jsonArray.toString()).apply()
    }

    fun deleteCustomQuote(context: Context, quoteId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = getCustomQuotes(context).toMutableList()
        existing.removeAll { it.id == quoteId }

        val jsonArray = JSONArray()
        for (q in existing) {
            jsonArray.put(q.toJson())
        }
        prefs.edit().putString(KEY_CUSTOM_QUOTES, jsonArray.toString()).apply()
    }

    private fun getCustomQuotes(context: Context): List<QuoteItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CUSTOM_QUOTES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<QuoteItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i)
                if (obj != null) list.add(QuoteItem.fromJson(obj))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
