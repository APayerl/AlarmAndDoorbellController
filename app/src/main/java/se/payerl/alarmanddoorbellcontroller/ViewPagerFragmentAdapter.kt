package se.payerl.alarmanddoorbellcontroller

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerFragmentAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private var arrayList: MutableList<Fragment> = mutableListOf()

    fun addFragment(fragment: Fragment) {
        arrayList.add(fragment)
        this.notifyItemInserted(this.itemCount - 1)
    }

    fun removeFragment(index: Int) {
        arrayList.removeAt(index)
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    override fun createFragment(position: Int): Fragment {
        return arrayList[position]
    }
}