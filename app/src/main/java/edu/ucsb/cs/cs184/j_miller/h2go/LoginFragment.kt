package edu.ucsb.cs.cs184.j_miller.h2go

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.common.collect.Maps
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment() {

    companion object {
        fun newInstance() = LoginFragment()
    }

    private lateinit var viewModel: LoginViewModel
    private lateinit var errorText: TextView
    private lateinit var emailField: EditText
    private lateinit var pwordField: EditText
    private lateinit var signinButton: Button
    private lateinit var accountButton: Button
    private lateinit var closeButton: ImageButton
    private lateinit var auth: FirebaseAuth
    private lateinit var act: MapsActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.login_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        errorText = view.findViewById<TextView>(R.id.error_text)
        emailField = view.findViewById<EditText>(R.id.email_value)
        pwordField = view.findViewById<EditText>(R.id.pword_value)
        signinButton = view.findViewById<Button>(R.id.signin_button)
        accountButton = view.findViewById<Button>(R.id.account_button)
        closeButton = view.findViewById<ImageButton>(R.id.close_button)

        act = activity as MapsActivity
        auth = act.auth

        // restore state after rotation
        errorText.text = viewModel.errorText

        accountButton.setOnClickListener {
            if(emailField.text.isNotEmpty() && pwordField.text.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(
                    emailField.text.toString(),
                    pwordField.text.toString()
                )
                    .addOnCompleteListener(act) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            //Log.i("MapsActivity", "createUserWithEmail:success")
                            errorText.text = getString(R.string.create_account_success)
                            act.updateUI()
                        } else {
                            // If sign in fails, display a message to the user.
                            //Log.i("MapsActivity", "createUserWithEmail:failure", task.exception)
                            errorText.text = task.exception?.message.toString()
                            act.updateUI()
                        }
                    }
            }
        }

        signinButton.setOnClickListener {
            if(emailField.text.isNotEmpty() && pwordField.text.isNotEmpty()) {
                auth.signInWithEmailAndPassword(emailField.text.toString(), pwordField.text.toString())
                    .addOnCompleteListener(act) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            //Log.i("MapsActivity", "signInWithEmail:success")
                            errorText.text = getString(R.string.sign_in_success)
                            act.updateUI()
                        } else {
                            // If sign in fails, display a message to the user.
                            //Log.i("MapsActivity", "signInWithEmail:failure")
                            errorText.text = task.exception?.message.toString()
                            act.updateUI()
                        }
                    }
            }
        }

        // when close button clicked, close the fragment
        closeButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

    }

}