
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import timber.log.Timber
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Sumeet on 24,January,2022
This class helps you to return same view holder once bind , without rebinding again
Just sent the required type for enabling cache using enableCacheForViewHolderType and overriding getItemId()
*/
abstract class BaseAdapterWithCaching : RecyclerView.Adapter<ViewHolder>() {

    private val cachedItems: MutableMap<String, ViewHolder> = ConcurrentHashMap()
    private val cachedViewHolderTypes: MutableSet<Int> = HashSet()

    companion object {
        const val NO_CACHED_ITEM_ID = "NO_CACHED_ITEM_ID"
    }

    //Return cached view holder for particular position
    private val viewCacheExtension = object : ViewCacheExtension() {
        override fun getViewForPositionAndType(
            recycler: Recycler,
            position: Int,
            type: Int
        ): View? {
            val cachedItemId = getItemId(position)
            if (cachedViewHolderTypes.contains(type) && cachedItemId != NO_CACHED_ITEM_ID
                && cachedItems.containsKey(cachedItemId)
            ) {
                Timber.d("Returning view from custom cache for position:$position")
                return cachedItems[cachedItemId]?.itemView
            }
            return null
        }
    }

    fun enableCacheForViewHolderType(type: Int) {
        cachedViewHolderTypes.add(type)
    }

    protected fun isCacheEnabledForViewHolder(type: Int): Boolean =
        cachedViewHolderTypes.contains(type)

    private val adapterDataObserver: DataChangeObserver = object : DataChangeObserver() {   //This observers will take care of removing the cached items if they are not in new set
        override fun onChanged() {
            Timber.d("items added or moved")
            if (cachedItems.isEmpty())
                return

            val iterator: MutableIterator<String> = cachedItems.keys.iterator()

            val newItemsIds = mutableSetOf<String>()

            for (newIndex in 0 until itemCount) {
                val itemId = getItemId(newIndex)
                newItemsIds.add(itemId)
            }

            //Remove cached items which are not in new set
            while (iterator.hasNext()) {
                val cachedItemId = iterator.next()
                val holder = cachedItems[cachedItemId]
                if (holder?.bindingAdapterPosition == -1) {
                    cachedItems[cachedItemId]?.itemView?.visibility = View.GONE
                    iterator.remove()
                }
                if (!newItemsIds.contains(cachedItemId)) {
                    cachedItems[cachedItemId]?.itemView?.visibility = View.GONE
                    iterator.remove()
                }
            }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            Timber.d("On Item range removed  called for adapter observer")
            if (cachedItems.isEmpty())
                return
            val iterator: MutableIterator<String> = cachedItems.keys.iterator()

            while (iterator.hasNext()) {
                val cachedItemId = iterator.next()
                val index = cachedItems[cachedItemId]?.bindingAdapterPosition ?: -1
                if (index != -1 && positionStart <= index && index < positionStart + itemCount) {
                    Timber.d("Removing view from adapter observer for position:$index")
                    cachedItems[cachedItemId]?.itemView?.visibility = View.GONE
                    iterator.remove()
                }
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        registerAdapterDataObserver(adapterDataObserver)
        recyclerView.setViewCacheExtension(viewCacheExtension)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        Timber.d("onDetached recycler view called ")
        unregisterAdapterDataObserver(adapterDataObserver)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    //Saving the cached item once it goes out of screen
    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        if (cachedViewHolderTypes.contains(holder.itemViewType)) {
            val pos = holder.bindingAdapterPosition
            Timber.d("onViewDetached called for position : $pos")
            if (pos > -1) {
                holder.setIsRecyclable(false)
                cachedItems[getItemId(pos)] = holder
            }

        }
        super.onViewDetachedFromWindow(holder)
    }

    private abstract class DataChangeObserver : AdapterDataObserver() {
        abstract override fun onChanged()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            onChanged()
        }

        override fun onItemRangeChanged(
            positionStart: Int, itemCount: Int,
            payload: Any?
        ) {
            onChanged()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            onChanged()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            onChanged()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            onChanged()
        }
    }

}
