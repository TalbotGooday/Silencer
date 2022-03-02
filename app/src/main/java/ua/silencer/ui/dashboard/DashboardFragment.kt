package ua.silencer.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ua.silencer.R
import ua.silencer.databinding.FragmentDashboardBinding
import ua.silencer.ui.getColorFromAttr
import ua.silencer.ui.service.SilencerService
import ua.silencer.ui.setFadedBackgroundColor
import ua.silencer.ui.status.StatusActivity

class DashboardFragment : Fragment() {
    companion object {
        private const val URL_PATTERN =
            "(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?([a-zA-Z0-9]+([\\-\\.][a-zA-Z0-9]+)*\\.[a-zA-Z]{2,5}(:[0-9]{1,5})?(?:\\/?\\S*)?)"
        private const val IP_PATTERN =
            "((?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.?){4}(?:\\:\\d{1,4})?)"
        const val FIND_PATTERN = "$URL_PATTERN|$IP_PATTERN"
    }

    private lateinit var dashboardViewModel: DashboardViewModel
    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.start.setOnClickListener { startService() }
    }

    private fun startService() {
        val rawText = binding.text.text.toString()
        if (rawText.isEmpty()) {
            binding.text.setFadedBackgroundColor(
                requireContext().getColorFromAttr(R.attr.colorOnPrimary).let {
                    ColorUtils.setAlphaComponent(it, 100)
                })

            return
        }

        val addressees = rawText
            .let { text ->
                FIND_PATTERN.toRegex().findAll(text).map { it.value }.toList()
            }
            .asSequence()
            .filter {
                it.contains(".ua").not()
            }.map { url ->
                if (url.startsWith("http").not()) {
                    "http://$url"
                } else url
            }.toSet().toTypedArray()

        binding.text.setText(addressees.joinToString("\n"))

        if (addressees.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_addresses),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val context = requireContext()

        val intent = Intent(context, SilencerService::class.java)
        intent.putExtra("addressees", addressees)

        context.stopService(intent)

        ContextCompat.startForegroundService(context, intent)

        startActivity(StatusActivity.newIntent(requireContext(), addressees))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}