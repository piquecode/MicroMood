package com.piquecode.micromood.about

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.piquecode.micromood.R

class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) =
        setPreferencesFromResource(R.xml.preferences, rootKey)
}
