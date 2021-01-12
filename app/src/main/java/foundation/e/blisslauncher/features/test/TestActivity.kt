package foundation.e.blisslauncher.features.test

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import foundation.e.blisslauncher.R
import kotlinx.android.synthetic.main.activity_test.*

class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        val tv1 = layoutInflater.inflate(R.layout.test_page_view, null)
        tv1.findViewById<FrameLayout>(R.id.root).setBackgroundColor(Color.RED)
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        tv1.layoutParams = layoutParams
        testView.addView(tv1)
        val tv2 = layoutInflater.inflate(R.layout.test_page_view, null)
        tv2.findViewById<FrameLayout>(R.id.root).setBackgroundColor(Color.GREEN)
        tv2.layoutParams = layoutParams
        testView.addView(tv2)
        val tv3 = layoutInflater.inflate(R.layout.test_page_view, null)
        tv3.findViewById<FrameLayout>(R.id.root).setBackgroundColor(Color.BLUE)
        tv3.layoutParams = layoutParams
        testView.addView(tv3)
    }
}