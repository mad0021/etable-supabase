package cat.dam.mamadou.etable.ui.components

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Formatea un importe en euros según el idioma actual (p.ej. "12,50 €" o "€12.50"). */
fun formatPrice(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        currency = Currency.getInstance("EUR")
    }.format(value)
