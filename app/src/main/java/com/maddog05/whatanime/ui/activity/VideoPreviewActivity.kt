package com.maddog05.whatanime.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.media3.common.Player
import com.devbrackets.android.exomedia.listener.OnCompletionListener
import com.devbrackets.android.exomedia.listener.OnPreparedListener
import com.maddog05.maddogutilities.android.Checkers
import com.maddog05.whatanime.R
import com.maddog05.whatanime.core.entity.SearchImageResult
import com.maddog05.whatanime.databinding.ActivityVideoPreviewBinding
import com.maddog05.whatanime.util.C
import com.maddog05.whatanime.util.Mapper
import es.dmoral.toasty.Toasty

class VideoPreviewActivity : AppCompatActivity(R.layout.activity_video_preview), OnPreparedListener,
    OnCompletionListener {
    private var videoUrl = C.EMPTY
    private var doc: SearchImageResult? = null

    private lateinit var binding: ActivityVideoPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupExtraData()

        setSupportActionBar(binding.toolbarVideoPreview)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.videoViewPreview.setOnPreparedListener(this)
        binding.videoViewPreview.setOnCompletionListener(this)
        binding.btnShareVideoPreview.setOnClickListener { actionShare() }
        binding.btnExpandVideoPreview.setOnClickListener { actionExpand() }
        setupData()
    }

    override fun onPause() {
        super.onPause()
        if (binding.videoViewPreview.isPlaying) {
            binding.videoViewPreview.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (binding.pbarLoadingVideoPreview.isGone && !binding.videoViewPreview.isPlaying) binding.videoViewPreview.start()
    }

    private fun setupExtraData() {
        val bundle = intent.extras
        if (bundle != null) {
            videoUrl = bundle.getString(C.Extras.VIDEO_URL, C.EMPTY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                doc = bundle.getParcelable(C.Extras.DOC, SearchImageResult::class.java)
            } else {
                @Suppress("DEPRECATION")
                doc = bundle.getParcelable(C.Extras.DOC)
            }
        }
    }

    private fun setupData() {
        val title = doc?.filename ?: ""
        setupTitle(title)
        if (Checkers.isInternetInWifiOrData(this@VideoPreviewActivity)) {
            binding.videoViewPreview.setMedia(videoUrl.toUri())
            binding.videoViewPreview.setRepeatMode(Player.REPEAT_MODE_ALL)
//            binding.videoViewPreview.setVideoURI(Uri.parse(videoUrl))
        } else {
            showError(getString(R.string.error_internet_connection))
        }
    }

    private fun showError(text: String) {
        Toasty.error(this@VideoPreviewActivity, text, Toast.LENGTH_SHORT).show()
    }

    private fun setupTitle(text: String) {
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.title = text
        }
    }

    @Suppress("DEPRECATION")
    private fun actionExpand() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            val controller = window.insetsController
            val insets = window.decorView.rootWindowInsets
            val isNotFullScreen =
                insets?.isVisible(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()) == true

            if (isNotFullScreen) {
                goFullscreen()
            } else {
                exitFullscreen()
            }
        } else {
            // Soporte para API < 30
            val isNotFullScreen = window.decorView.systemUiVisibility == View.SYSTEM_UI_FLAG_VISIBLE
            if (isNotFullScreen) {
                goFullscreen()
            } else {
                exitFullscreen()
            }
        }
    }

    private fun actionShare() {
        val nameAndEpisodeText = Mapper.parseEpisodeNumber(this, doc!!.episode)
        val title = doc?.filename ?: ""
        val text = (title
                + C.SPACE
                + nameAndEpisodeText
                + C.SPACE
                + getString(R.string.share_founded_with)
                + C.SPACE
                + getString(R.string.app_name))
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    public override fun onDestroy() {
        super.onDestroy()
        exitFullscreen()
    }

    private fun goFullscreen() {
        setUiFlags(true)
    }

    private fun exitFullscreen() {
        setUiFlags(false)
    }

    private fun setUiFlags(fullscreen: Boolean) {
        if (fullscreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.systemBars())
                window.insetsController?.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.show(WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    override fun onPrepared() {
        binding.pbarLoadingVideoPreview.visibility = View.GONE
        binding.videoViewPreview.start()
    }

    override fun onCompletion() {
        binding.pbarLoadingVideoPreview.visibility = View.GONE
        binding.videoViewPreview.restart()
    }

    //BACK
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) onBackPressedDispatcher.onBackPressed()
        return super.onOptionsItemSelected(item)
    }
}