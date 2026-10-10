import cat.dam.mamadou.etable.R

enum class AppTheme {
     LIGHT, DARK, OCEAN, SUNSET, NATURE, PURPLE,
    MONOCHROME, AUTUMN, VINTAGE, FIRE, MIDNIGHT, FOREST;
    
    // Esta función será usada para obtener el ID del recurso de string
    fun getNameResourceId(): Int {
        return when (this) {
            LIGHT -> R.string.theme_light
            DARK -> R.string.theme_dark
            OCEAN -> R.string.theme_ocean
            SUNSET -> R.string.theme_sunset
            NATURE -> R.string.theme_nature
            PURPLE -> R.string.theme_purple
            MONOCHROME -> R.string.theme_monochrome
            AUTUMN -> R.string.theme_autumn
            VINTAGE -> R.string.theme_vintage
            FIRE -> R.string.theme_fire
            MIDNIGHT -> R.string.theme_midnight
            FOREST -> R.string.theme_forest
        }
    }
}