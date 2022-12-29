package com.vkas.translationapp.ui.list

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.translationapp.BR
import com.vkas.translationapp.R
import com.vkas.translationapp.app.App
import com.vkas.translationapp.base.BaseActivity
import com.vkas.translationapp.bean.PtVpnBean
import com.vkas.translationapp.databinding.ActivityListPtBinding
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.utils.KLog
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ListPtActivity : BaseActivity<ActivityListPtBinding, ListViewModel>() {
    private lateinit var selectAdapter: ListPtAdapter
    private var ptServiceBeanList: MutableList<PtVpnBean> = ArrayList()
    private lateinit var adBean: PtVpnBean

    private var jobBackPt: Job? = null

    //选中服务器
    private lateinit var checkSkServiceBean: PtVpnBean
    private lateinit var checkSkServiceBeanClick: PtVpnBean

    // 是否连接
    private var whetherToConnect = false
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_list_pt
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        checkSkServiceBean = PtVpnBean()
        whetherToConnect = bundle?.getBoolean(Constant.WHETHER_PT_CONNECTED) == true
        checkSkServiceBean = JsonUtil.fromJson(
            bundle?.getString(Constant.CURRENT_PT_SERVICE),
            object : TypeToken<PtVpnBean?>() {}.type
        )
        checkSkServiceBeanClick = checkSkServiceBean
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.selectTitle.imgBack.setImageResource(R.drawable.ic_title_back)
        binding.selectTitle.tvTitle.text = (getString(R.string.servers))
        binding.selectTitle.tvRight.visibility = View.GONE
        binding.selectTitle.imgBack.setOnClickListener {
            finish()
        }
    }

    override fun initData() {
        super.initData()
        initSelectRecyclerView()
        viewModel.getServerListData()
    }

    override fun initViewObservable() {
        super.initViewObservable()
        getServerListData()
    }

    private fun getServerListData() {
        viewModel.liveServerListData.observe(this, {
            echoServer(it)
        })
    }

    private fun initSelectRecyclerView() {
        selectAdapter = ListPtAdapter(ptServiceBeanList)
        val layoutManager =
            GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false)

        binding.recyclerSelect.layoutManager = layoutManager
        binding.recyclerSelect.adapter = selectAdapter
        selectAdapter.setOnItemClickListener { _, _, pos ->
            run {
                selectServer(pos)
            }
        }
    }

    /**
     * 选中服务器
     */
    private fun selectServer(position: Int) {
        if (ptServiceBeanList[position].pt_ip == checkSkServiceBeanClick.pt_ip && ptServiceBeanList[position].pt_best == checkSkServiceBeanClick.pt_best) {
            if (!whetherToConnect) {
                finish()
                LiveEventBus.get<PtVpnBean>(Constant.NOT_CONNECTED_RETURN)
                    .post(checkSkServiceBean)
            }
            return
        }
        ptServiceBeanList.forEachIndexed { index, _ ->
            ptServiceBeanList[index].pt_check = position == index
            if (ptServiceBeanList[index].pt_check == true) {
                checkSkServiceBean = ptServiceBeanList[index]
            }
        }
        selectAdapter.notifyDataSetChanged()
        showDisconnectDialog()
    }

    /**
     * 回显服务器
     */
    private fun echoServer(it: MutableList<PtVpnBean>) {
        ptServiceBeanList = it
        ptServiceBeanList.forEachIndexed { index, _ ->
            if (checkSkServiceBeanClick.pt_best == true) {
                ptServiceBeanList[0].pt_check = true
            } else {
                ptServiceBeanList[index].pt_check =
                    ptServiceBeanList[index].pt_ip == checkSkServiceBeanClick.pt_ip
                ptServiceBeanList[0].pt_check = false
            }
        }
        KLog.e("TAG", "ptServiceBeanList=${JsonUtil.toJson(ptServiceBeanList)}")
        selectAdapter.setList(ptServiceBeanList)
    }

    /**
     * 返回主页
     */
    private fun returnToHomePage() {
        finish()
    }

    /**
     * 是否断开连接
     */
    private fun showDisconnectDialog() {
        if (!whetherToConnect) {
            finish()
            LiveEventBus.get<PtVpnBean>(Constant.NOT_CONNECTED_RETURN)
                .post(checkSkServiceBean)
            return
        }
        val dialog: android.app.AlertDialog? = android.app.AlertDialog.Builder(this)
            .setTitle("Are you sure to disconnect current server")
            //设置对话框的按钮
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
                ptServiceBeanList.forEachIndexed { index, _ ->
                    ptServiceBeanList[index].pt_check =
                        (ptServiceBeanList[index].pt_ip == checkSkServiceBeanClick.pt_ip && ptServiceBeanList[index].pt_best == checkSkServiceBeanClick.pt_best)
                }
                selectAdapter.notifyDataSetChanged()
            }
            .setPositiveButton("DISCONNECT") { dialog, _ ->
                dialog.dismiss()
                finish()
                LiveEventBus.get<PtVpnBean>(Constant.CONNECTED_RETURN)
                    .post(checkSkServiceBean)
            }.create()

        val params = dialog!!.window!!.attributes
        params.width = 200
        params.height = 200
        dialog.window!!.attributes = params
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }

}