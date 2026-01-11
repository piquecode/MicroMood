package com.piquecode.micromood.about

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.piquecode.micromood.R
import com.piquecode.micromood.databinding.ActivityAboutBinding
import com.piquecode.micromood.util.Constants.EMAIL
import com.piquecode.micromood.util.Constants.MAIL_TO
import com.piquecode.micromood.util.Constants.REPO
import com.piquecode.micromood.util.Constants.WEBSITE
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialiseView()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun initialiseView() {
        title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.elevation = 0f

        binding.aboutWrapper.setBackgroundColor(
            ContextCompat.getColor(this, R.color.colorBackground)
        )

        binding.contact.setOnClickListener { startEmailIntent() }
        binding.website.setOnClickListener { startWebsiteIntent(WEBSITE) }
        binding.contribute.setOnClickListener { startWebsiteIntent(REPO) }
        binding.appIcon.setOnLongClickListener { showConfetti(it) }
    }

    private fun startEmailIntent() {
        val title = getString(R.string.query)
        val uri = Uri.parse(MAIL_TO)
        val emailIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        startActivity(Intent.createChooser(emailIntent, title))
    }

    private fun startWebsiteIntent(website: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(website))
        startActivity(intent)
    }

    private fun showConfetti(it: View): Boolean {
        val location = IntArray(2)
        it.getLocationInWindow(location)

        val drawable1 = ContextCompat.getDrawable(this, R.drawable.ic_mood_1)?.apply {
            setTint(ContextCompat.getColor(this@AboutActivity, R.color.colorMood1))
        }
        val drawable2 = ContextCompat.getDrawable(this, R.drawable.ic_mood_2)?.apply {
            setTint(ContextCompat.getColor(this@AboutActivity, R.color.colorMood2))
        }
        val drawable3 = ContextCompat.getDrawable(this, R.drawable.ic_mood_3)?.apply {
            setTint(ContextCompat.getColor(this@AboutActivity, R.color.colorMood3))
        }
        val drawable4 = ContextCompat.getDrawable(this, R.drawable.ic_mood_4)?.apply {
            setTint(ContextCompat.getColor(this@AboutActivity, R.color.colorMood4))
        }
        val drawable5 = ContextCompat.getDrawable(this, R.drawable.ic_mood_5)?.apply {
            setTint(ContextCompat.getColor(this@AboutActivity, R.color.colorMood5))
        }

        val shapes = mutableListOf<Shape>()
        drawable1?.let { shapes.add(Shape.DrawableShape(it, false)) }
        drawable2?.let { shapes.add(Shape.DrawableShape(it, false)) }
        drawable3?.let { shapes.add(Shape.DrawableShape(it, false)) }
        drawable4?.let { shapes.add(Shape.DrawableShape(it, false)) }
        drawable5?.let { shapes.add(Shape.DrawableShape(it, false)) }

        if (shapes.isEmpty()) {
            shapes.add(Shape.Circle)
        }

        binding.confetti.build()
            .setDirection(0.0, 360.0)
            .setSpeed(7f, 20f)
            .setFadeOutEnabled(true)
            .setTimeToLive(2000L)
            .addShapes(*shapes.toTypedArray())
            .addSizes(Size(12))
            .setPosition(location[0].toFloat() + it.width / 2, location[1].toFloat())
            .burst(100)

        return true
    }
}