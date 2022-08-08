package com.hotdldl.session001

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment

class ImageViewFragment : Fragment() {

    private lateinit var ivAvatar: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ivAvatar = view.findViewById(R.id.iv_avatar)
        // 获取图片
        val bitmap = BitmapFactory.decodeStream(context?.assets?.open("avatar.jpg"))
        bitmap?.let {
            ivAvatar.setImageBitmap(it)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ImageViewFragment()
    }
}