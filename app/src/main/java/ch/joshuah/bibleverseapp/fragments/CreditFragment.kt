package ch.joshuah.bibleverseapp.fragments

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import ch.joshuah.bibleverseapp.R


class CreditFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_credit, container, false)

        val t = view.findViewById<TextView>(R.id.fragment_credit_textViewCreditText)
        t?.movementMethod = LinkMovementMethod.getInstance()

        return view
    }
}