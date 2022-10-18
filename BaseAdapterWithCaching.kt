/**
 * Created by Sumeet on 24,January,2022
This class helps you to return same view holder once bind , without rebinding again
Just sent the required type for enabling cache using enableCacheForViewHolderType and overriding getItemId()
 */
abstract class BaseAdapterWithCaching : RecyclerView.Adapter<ViewHolder>() {

    abstract fun getCachedItemId(position: Int): String
    //Return unique id from this for items which you want to cache , override this in child class and replace getItemId in base class with getCachedItemId()

    private val cachedItems: MutableMap<String, ViewHolder> = ConcurrentHashMap()
    private val cachedViewHolderTypes: MutableSet<Int> = HashSet()

    companion object {
        const val NO_CACHED_ITEM_ID = "NO_CACHED_ITEM_ID"
        const val TAG = "BaseAdapterWithCaching"
    }

    //Return cached view holder for particular position
    private val viewCacheExtension = object : ViewCacheExtension() {
        override fun getViewForPositionAndType(
            recycler: Recycler,
            position: Int,
            type: Int
        ): View? {
            val cachedItemId = getCachedItemId(position)
            if (cachedViewHolderTypes.contains(type) && cachedItemId != NO_CACHED_ITEM_ID
                && cachedItems.containsKey(cachedItemId)
            ) {
                Log.d(TAG, "Returning view from custom cache for position:$position")
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

    private val adapterDataObserver: DataChangeObserver = object :
        DataChangeObserver() {   //This observers will take care of removing the cached items if they are not in new set
        override fun onChanged() {
            Log.d(TAG, "items added or moved")
            if (cachedItems.isEmpty())
                return

            val iterator: MutableIterator<String> = cachedItems.keys.iterator()

            val newItemsIds = mutableSetOf<String>()

            for (newIndex in 0 until itemCount) {
                val itemId = getCachedItemId(newIndex)
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
            Log.d(TAG, "On Item range removed  called for adapter observer")
            if (cachedItems.isEmpty())
                return
            val iterator: MutableIterator<String> = cachedItems.keys.iterator()

            while (iterator.hasNext()) {
                val cachedItemId = iterator.next()
                val index = cachedItems[cachedItemId]?.bindingAdapterPosition ?: -1
                if (index != -1 && positionStart <= index && index < positionStart + itemCount) {
                    Log.d(TAG, "Removing view from adapter observer for position:$index")
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
        Log.d(TAG, "onDetached recycler view called ")
        unregisterAdapterDataObserver(adapterDataObserver)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    //Saving the cached item once it goes out of screen
    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        if (cachedViewHolderTypes.contains(holder.itemViewType)) {
            val pos = holder.bindingAdapterPosition
            Log.d(TAG, "onViewDetached called for position : $pos")
            if (pos > -1) {
                holder.setIsRecyclable(false)
                cachedItems[getCachedItemId(pos)] = holder
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
