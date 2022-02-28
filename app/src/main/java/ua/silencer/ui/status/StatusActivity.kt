package ua.silencer.ui.status

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ua.silencer.R
import ua.silencer.databinding.ActivityStatusBinding
import ua.silencer.ui.service.SilencerService
import ua.silencer.ui.status.adapters.StatusAdapter

class StatusActivity : AppCompatActivity() {
    companion object {
        fun newIntent(context: Context, addresses: Array<String>): Intent {
            return Intent(context, StatusActivity::class.java).apply {
                putExtra("addresses", addresses)
            }
        }
    }

    private var _binding: ActivityStatusBinding? = null
    private val binding
        get() = _binding!!

    private val statusAdapter = createStatusAdapter()
    private val silencerMessageReceiver = createStatusReceiver()

    override fun onStart() {
        super.onStart()
        registerReceiver(silencerMessageReceiver, IntentFilter(SilencerService.SILENCER_EVENT))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityStatusBinding.inflate(layoutInflater)

        setContentView(binding.root)

        initViews()

        handleData(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleData(intent)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(silencerMessageReceiver)
    }

    private fun createStatusReceiver() = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            handleSilencerMessage(intent)
        }
    }

    private fun handleSilencerMessage(intent: Intent) {
        when (intent.getStringExtra("event")) {
            SilencerService.EVENT_MESSAGE -> {
                val address = intent.getStringExtra("address") ?: return
                val isAlive = intent.getBooleanExtra("isAlive", false)

                statusAdapter.updateStatus(address, isAlive)

                updateStatus(address, isAlive)
            }
            SilencerService.EVENT_STOP -> {
                Toast.makeText(this, getString(R.string.done), Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun updateStatus(address: String, alive: Boolean) = with(binding) {
        addressInProgress.text = getString(R.string.working_on, address)
    }

    private fun initViews() {
        binding.addresses.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = statusAdapter
        }
    }

    private fun handleData(intent: Intent?) {
        val addresses = intent?.getStringArrayExtra("addresses")?.map {
            StatusAdapter.AddressModel(it, StatusAdapter.Status.UNKNOWN)
        } ?: emptyList()

        statusAdapter.swapData(addresses)
    }

    private fun createStatusAdapter(): StatusAdapter {
        return StatusAdapter(object : StatusAdapter.Listener {
            override fun onItemClick(item: StatusAdapter.AddressModel) {
                openLink(item.address)
            }

            override fun onItemStatusClick(item: StatusAdapter.AddressModel) {
                onStatusClick(item)
            }
        })
    }

    private fun onStatusClick(item: StatusAdapter.AddressModel) {
        val message = when(item.status){
            StatusAdapter.Status.UNKNOWN -> R.string.status_unknown
            StatusAdapter.Status.ACTIVE -> R.string.status_active
            StatusAdapter.Status.DOWN -> R.string.status_down
        }

        Toast.makeText(this, getString(message), Toast.LENGTH_SHORT).show()
    }

    private fun openLink(item: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item))
        startActivity(browserIntent)
    }
}