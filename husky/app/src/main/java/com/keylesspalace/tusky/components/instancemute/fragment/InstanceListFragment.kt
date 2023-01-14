package com.keylesspalace.tusky.components.instancemute.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.components.instancemute.adapter.DomainMutesAdapter
import com.keylesspalace.tusky.components.instancemute.interfaces.InstanceActionListener
import com.keylesspalace.tusky.databinding.FragmentInstanceListBinding
import com.keylesspalace.tusky.fragment.BaseFragment
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.HttpHeaderLink
import com.keylesspalace.tusky.util.hide
import com.keylesspalace.tusky.util.show
import com.keylesspalace.tusky.view.EndlessOnScrollListener
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.from
import com.uber.autodispose.autoDispose
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.android.ext.android.inject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class InstanceListFragment : BaseFragment(), InstanceActionListener {

    private val binding by viewBinding(FragmentInstanceListBinding::bind)
    private val api: MastodonApi by inject()
    private var fetching = false
    private var bottomId: String? = null
    private var adapter = DomainMutesAdapter(this)
    private lateinit var scrollListener: EndlessOnScrollListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                view.context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.recyclerView.adapter = adapter

        val layoutManager = LinearLayoutManager(view.context)
        binding.recyclerView.layoutManager = layoutManager

        scrollListener = object : EndlessOnScrollListener(layoutManager) {
            override fun onLoadMore(totalItemsCount: Int, view: RecyclerView) {
                if (bottomId != null) {
                    fetchInstances(bottomId)
                }
            }
        }

        binding.recyclerView.addOnScrollListener(scrollListener)
        fetchInstances()
    }

    override fun mute(mute: Boolean, instance: String, position: Int) {
        if (mute) {
            api.blockDomain(instance).enqueue(object : Callback<Any> {
                override fun onFailure(call: Call<Any>, t: Throwable) {
                    Log.e(TAG, "Error muting domain $instance")
                }

                override fun onResponse(call: Call<Any>, response: Response<Any>) {
                    if (response.isSuccessful) {
                        adapter.addItem(instance)
                    } else {
                        Log.e(TAG, "Error muting domain $instance")
                    }
                }
            })
        } else {
            api.unblockDomain(instance).enqueue(object : Callback<Any> {
                override fun onFailure(call: Call<Any>, t: Throwable) {
                    Log.e(TAG, "Error unmuting domain $instance")
                }

                override fun onResponse(call: Call<Any>, response: Response<Any>) {
                    if (response.isSuccessful) {
                        adapter.removeItem(position)
                        Snackbar.make(
                            binding.recyclerView,
                            getString(R.string.confirmation_domain_unmuted, instance),
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(R.string.action_undo) {
                                mute(true, instance, position)
                            }
                            .show()
                    } else {
                        Log.e(TAG, "Error unmuting domain $instance")
                    }
                }
            })
        }
    }

    private fun fetchInstances(id: String? = null) {
        if (fetching) {
            return
        }
        fetching = true
        binding.instanceProgressBar.show()

        if (id != null) {
            binding.recyclerView.post { adapter.bottomLoading = true }
        }

        api.domainBlocks(id, bottomId)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(from(this, Lifecycle.Event.ON_DESTROY))
            .subscribe({ response ->
                val instances = response.body()

                if (response.isSuccessful && instances != null) {
                    onFetchInstancesSuccess(instances, response.headers().get("Link"))
                } else {
                    onFetchInstancesFailure(Exception(response.message()))
                }
            }, { throwable ->
                onFetchInstancesFailure(throwable)
            })
    }

    private fun onFetchInstancesSuccess(instances: List<String>, linkHeader: String?) {
        adapter.bottomLoading = false
        binding.instanceProgressBar.hide()

        val links = HttpHeaderLink.parse(linkHeader)
        val next = HttpHeaderLink.findByRelationType(links, "next")
        val fromId = next?.uri?.getQueryParameter("max_id")
        adapter.addItems(instances)
        bottomId = fromId
        fetching = false

        if (adapter.itemCount == 0) {
            binding.messageView.show()
            binding.messageView.setup(
                R.drawable.elephant_friend_empty,
                R.string.message_empty,
                null
            )
        } else {
            binding.messageView.hide()
        }
    }

    private fun onFetchInstancesFailure(throwable: Throwable) {
        fetching = false
        binding.instanceProgressBar.hide()
        Log.e(TAG, "Fetch failure", throwable)

        if (adapter.itemCount == 0) {
            binding.messageView.show()
            if (throwable is IOException) {
                binding.messageView.setup(R.drawable.elephant_offline, R.string.error_network) {
                    binding.messageView.hide()
                    this.fetchInstances(null)
                }
            } else {
                binding.messageView.setup(R.drawable.elephant_error, R.string.error_generic) {
                    binding.messageView.hide()
                    this.fetchInstances(null)
                }
            }
        }
    }

    companion object {
        private const val TAG = "InstanceList" // logging tag
    }
}
