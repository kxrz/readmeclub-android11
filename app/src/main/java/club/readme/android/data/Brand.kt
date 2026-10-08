package club.readme.android.data

data class Brand(
    val slug: String,
    val name: String,
    /** Logo `src` (also its cache key), or null. */
    val logo: String?,
)
