package com.technokratzz.loadmoreapapter.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.technokratzz.loadmoreapapter.R
import com.technokratzz.loadmoreapapter.helper.EndlessScrollListener


abstract class LoadMoreAdapter<T>(val recyclerView: RecyclerView, val dataSet: ArrayList<T?>?, listener: LoadMoreAdapterListener?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val loader = 80801
    val normal = 80802
    private var tag = ""
    private var loadMore = true
    private var showToast = true
    private lateinit var scrollListener: EndlessScrollListener

    init {
        if (listener != null) {
            scrollListener = object : EndlessScrollListener(recyclerView.layoutManager) {
                override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?, isNetworkReady: Boolean) {
                    if (loadMore) {
                        if (isNetworkReady) {
                            showToast = true
                            listener.onLoadMore(page, totalItemsCount, view)
                            addLoader()
                        } else if (showToast) {
                            showToast = false
                            Toast.makeText(recyclerView.context, recyclerView.context.getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            recyclerView.addOnScrollListener(scrollListener)

            if (recyclerView.layoutManager is GridLayoutManager) {
                (recyclerView.layoutManager as GridLayoutManager).spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (getItemViewType(position)) {
                            loader -> 2
                            else -> 1
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType == loader) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.load_more_recycler, parent, false)
            val lm = recyclerView.layoutManager
            val frameLayout: FrameLayout = v.findViewById(R.id.loadMoreProgressLayout)
            val orientation = when (lm) {
                is LinearLayoutManager -> {
                    lm.orientation
                }
                is GridLayoutManager -> {
                    lm.orientation
                }
                else -> {
                    -1
                }
            }
            if(orientation == RecyclerView.HORIZONTAL) {
                frameLayout.layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            } else {
                frameLayout.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }
            ProgressViewHolder(v)
        } else {
            onCreateViewHolderCustom(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder !is ProgressViewHolder)
            onBindViewHolderCustom(holder, position)
    }

    override fun getItemViewType(position: Int): Int {
        var loaderPosition = position - getAddCount()
        if(loaderPosition < 0)
            loaderPosition = 0

        return when {
            dataSet != null && loaderPosition >= 0 && loaderPosition < dataSet.size && dataSet[loaderPosition] == null -> {
                loader
            }
            else -> {
                getItemViewTypeCustom(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return dataSet!!.size
    }

    private fun addLoader() {
        val item: T? = null
        if (!dataSet!!.contains(item)) {
            dataSet.add(item)
            recyclerView.post { notifyDataSetChanged() }
        } else {
            stopItemLoader(notify = false, addLoader = true)
        }
    }

    fun stopItemLoader(notify: Boolean = true, addLoader: Boolean = false) {
        if(notify) {
            recyclerView.post {
                val item: T? = null
                val index = dataSet!!.indexOf(item)
                if(index != -1) {
                    dataSet.remove(item)
                    notifyItemRemoved(index + getAddCount())
                }
            }
        } else {
            val item: T? = null
            val index = dataSet!!.indexOf(item)
            if(index != -1) {
                dataSet.remove(item)
            }
        }

        if(addLoader)
            addLoader()
    }

    private fun getAddCount() : Int {
        var addCount = itemCount - dataSet!!.size
        if(addCount < 0)
            addCount = 0
        return addCount
    }

    fun addMore(dataSet: List<T?>) {
        stopItemLoader(notify = false, addLoader = false)
        this.dataSet!!.addAll(dataSet)
        try {
            notifyItemRangeInserted(itemCount, dataSet.size)
        } catch (e: Exception) {
            Log.e("LoadMoreAdapter", "Custom Log Add more: $e")
            recyclerView.post { notifyDataSetChanged() }
        }
    }

    fun addNewList(dataSet: List<T?>) {
        this.dataSet?.clear()
        loadMore = true
        resetState()
        this.dataSet?.addAll(dataSet)
        notifyDataSetChanged()
    }

    fun clearRecyclerView() {
        this.dataSet?.clear()
        loadMore = true
        resetState()
        notifyDataSetChanged()
    }

    fun notifyItemChange(item: T?) {
        val index = dataSet!!.indexOf(item)
        if(index != -1)
            recyclerView.post { notifyItemChanged(index + getAddCount()) }
    }

    fun removeItem(item: T?) {
        val index = dataSet!!.indexOf(item)
        if(index != -1) {
            dataSet.removeAt(index)
            notifyItemRemoved(index + getAddCount())
        }
    }

    fun isLoadMoreEnabled(loadMore: Boolean) {
        this.loadMore = loadMore
        stopItemLoader(notify = true, addLoader = false)
    }

    fun resetState() {
        if (::scrollListener.isInitialized)
            scrollListener.resetState()
    }

    internal fun setTag(tag: String) {
        this.tag = tag
    }

    internal fun getTag() = tag

    interface LoadMoreAdapterListener {
        fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?)
    }

    abstract fun onCreateViewHolderCustom(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder

    abstract fun onBindViewHolderCustom(holder: RecyclerView.ViewHolder, position: Int)

    private class ProgressViewHolder(v: View) : RecyclerView.ViewHolder(v)

    open fun getItemViewTypeCustom(position: Int): Int {
        return normal
    }
}
