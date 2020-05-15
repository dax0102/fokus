package com.isaiahvonrundstedt.fokus.features.subject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.isaiahvonrundstedt.fokus.R
import com.isaiahvonrundstedt.fokus.features.shared.abstracts.BaseAdapter

class SubjectAdapter(private var actionListener: ActionListener,
                     private var swipeListener: SwipeListener)
    : BaseAdapter<SubjectAdapter.SubjectViewHolder>() {

    private var itemList = ArrayList<Subject>()

    fun setObservableItems(items: List<Subject>) {
        itemList.clear()
        itemList.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val rowView: View = LayoutInflater.from(parent.context).inflate(R.layout.layout_item_subject_card,
            parent, false)
        return SubjectViewHolder(rowView)
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.onBind(itemList[position])
    }

    override fun onSwipe(position: Int, direction: Int) {
        swipeListener.onSwipePerformed(position, itemList[position], direction)
    }

    inner class SubjectViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val rootView: View = itemView.findViewById(R.id.rootView)
        private val nameView: AppCompatTextView = itemView.findViewById(R.id.nameView)
        private val descriptionView: AppCompatTextView = itemView.findViewById(R.id.descriptionView)
        private val dateTimeView: AppCompatTextView = itemView.findViewById(R.id.dateTimeView)
        private val tagView: View = itemView.findViewById(R.id.tagView)

        fun onBind(subject: Subject) {
            rootView.setOnClickListener {
                actionListener.onActionPerformed(subject, ActionListener.Action.SELECT)
            }

            tagView.setBackgroundColor(subject.tag.actualColor)

            nameView.text = subject.code
            descriptionView.text = subject.description

            val builder = StringBuilder()
            val selectedDays = Subject.getDays(subject.daysOfWeek)
            selectedDays.forEachIndexed { index, dayOfWeek ->
                builder.append(itemView.context.getString(Subject.getDayNameResource(dayOfWeek)))

                if (index == selectedDays.size - 2)
                    builder.append(itemView.context.getString(R.string.and))
                else if (index < selectedDays.size - 2)
                    builder.append(", ")
            }
            builder.append(", ")
            builder.append(subject.formatStartTime())
                .append(" - ")
                .append(subject.formatEndTime())
            dateTimeView.text = builder.toString()

        }
    }
}