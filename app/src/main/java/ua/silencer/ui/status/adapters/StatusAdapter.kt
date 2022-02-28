package ua.silencer.ui.status.adapters

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ua.silencer.R
import ua.silencer.databinding.ItemAddressBinding
import ua.silencer.ui.getColorFromAttr
import ua.silencer.ui.status.adapters.StatusAdapter.Status.*

class StatusAdapter(private val listener: Listener) : RecyclerView.Adapter<StatusAdapter.Holder>() {

    private var data = mutableListOf<AddressModel>()

    private var lastUpdatedIndex = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)

        return Holder(
            ItemAddressBinding.inflate(inflater, parent, false)
        )
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(data[position], position == lastUpdatedIndex, listener)
    }

    fun addData(data: List<AddressModel>) {
        val res = DiffUtil.calculateDiff(StatusDiff(data, ArrayList(data)))

        this.data.clear()

        this.data.addAll(data)

        res.dispatchUpdatesTo(this)
    }

    fun swapData(data: List<AddressModel>) {
        val res = DiffUtil.calculateDiff(StatusDiff(data, ArrayList(data)))

        this.data.clear()

        this.data.addAll(data)

        res.dispatchUpdatesTo(this)
    }

    fun updateStatus(url: String, isActive: Boolean) {
        val newData = ArrayList(data)
        val oldData = ArrayList(data)

        val index = newData.indexOfFirst { it.address == url }

        if (index < 0) return

        lastUpdatedIndex = index

        newData[index] = newData[index].copy().apply {
            status = if (isActive) {
                ACTIVE
            } else {
                DOWN
            }
        }

        val res = DiffUtil.calculateDiff(
            StatusDiff(
                oldData,
                newData
            )
        )

        data.clear()
        data.addAll(newData)

        res.dispatchUpdatesTo(this)
    }

    class Holder(private val binding: ItemAddressBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AddressModel, isUpdatingNow: Boolean, listener: Listener) = with(binding) {
            address.text = item.address

            setStatus(item.status)

            setLastUpdated(isUpdatingNow)

            indicator.setOnClickListener {
                listener.onItemStatusClick(item)
            }
            root.setOnClickListener {
                listener.onItemClick(item)
            }
        }

        private fun setLastUpdated(updatingNow: Boolean) = with(binding) {
//            val color = if (updatingNow) {
//                root.context.getColorFromAttr(R.attr.colorOnPrimary)
//                    .let { ColorUtils.setAlphaComponent(it, 100) }
//            } else {
//                root.context.getColorFromAttr(R.attr.colorPrimaryVariant)
//            }
//
//            root.setBackgroundColor(color)
        }

        private fun setStatus(status: Status) = with(binding) {
            val color: Int = when (status) {
                UNKNOWN -> Color.GRAY
                ACTIVE -> Color.GREEN
                DOWN -> Color.RED
            }

            indicatorIcon.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    interface Listener {
        fun onItemClick(item: AddressModel)
        fun onItemStatusClick(item: AddressModel)
    }

    data class AddressModel(
        val address: String,
        var status: Status
    )

    enum class Status(val sortOrder: Int) {
        UNKNOWN(1),
        ACTIVE(2),
        DOWN(3)
    }
}