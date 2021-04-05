package li.gravio.gpsinjector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import li.gravio.common.LocationService
import java.util.*


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    var timer: Timer? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        timer = Timer(true)
        timer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                updateView()
            }
        },0, 2*1000)

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    fun updateView() {
        val status = LocationService.instance?.getStatus()
        if (status != null) {
            val textView = getView()?.findViewById(R.id.textView) as TextView
            if (textView != null) {

                var logs = ""
                for (lg in status.logBuffer.reversed()) {
                    val date = Date(lg.millis)
                    logs += "\n[${date.hours}:${date.minutes}:${date.seconds}] ${lg.level.name} ${lg.message}"
                }

                textView.text = ("Provider: ${status.provider}\n"
                        + "Msg: ${status.messages}\n"
                        + "Sat: ${status.satellites}\n"
                        + "Pos: ${status.location?.latitude?.toFloat()} ${status.location?.longitude?.toFloat()}\n"
                        + "Alt: ${status.location?.altitude?.toFloat()}\n"
                        + "Acc: ${status.location?.accuracy}\n"
                        + "Iat: ${status.location?.time?.let { Date(it) }}\n"
                        + "\n$logs")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_clienta).setOnClickListener {
            (activity as MainActivity?)?.startClient("A")
        }
        view.findViewById<Button>(R.id.button_clientb).setOnClickListener {
            (activity as MainActivity?)?.startClient("B")
        }
        view.findViewById<Button>(R.id.button_server).setOnClickListener {
            (activity as MainActivity?)?.startServer()
        }
    }

    override fun onDestroyView() {
        timer?.cancel()
        super.onDestroyView()
    }
}