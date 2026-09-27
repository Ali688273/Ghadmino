package ir.ghadmino.stepcounter.ads

/**
 * Production ad configuration for Ghadmino.
 *
 * App keys identify the two ad SDKs. Placement/zone ids identify the
 * individual ad slots. These are not Android secrets; they are client-side
 * identifiers supplied by the ad networks.
 */
object AdsConfig {
    const val TAPSELL_APP_KEY =
        "hfmftdctrtdblgddobrkecorgahjgtbdkopofapghnlhfsqgthaggoqlkgbrertdosblad"

    const val TAPSELL_REWARDED =
        "6ab8ba2c8f55014cb9ac9ff3"

    const val TAPSELL_INTERSTITIAL =
        "6ab8ba8d3df3c67e7db5713c"

    const val TAPSELL_PREVIEW_VIDEO =
        "6ab8bab53df3c67e7db5713d"

    const val TAPSELL_NATIVE_VIDEO =
        "6ab8bad57902f97efc2f3f53"

    const val TAPSELL_STANDARD_BANNER =
        "6ab8bb008f55014cb9ac9ff4"

    const val TAPSELL_INSTANT_BANNER =
        "6ab8bb278f55014cb9ac9ff5"

    const val TAPSELL_NATIVE_BANNER =
        "6ab8bb4e7902f97efc2f3f54"

    const val ADIVERY_APP_KEY =
        "5822a329-3759-43f4-8f24-bde669d2afa4"

    const val ADIVERY_INTERSTITIAL =
        "2308d203-4ab5-483a-84e0-563f18983827"

    const val ADIVERY_REWARDED =
        "96e4e4c8-6fbd-4a47-b2e5-925a723a0d20"

    const val ADIVERY_BANNER =
        "e5afe0ce-ab8c-47ec-ab6a-25bd60814f27"

    const val ADIVERY_APP_OPEN =
        "591ffe4c-7527-4718-8580-4d4627f27192"

    const val ADIVERY_PRE_ROLL =
        "87a4a7f7-2935-4c21-a553-abd01a750f9b"
}
