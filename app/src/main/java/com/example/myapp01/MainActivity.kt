package com.example.myapp01

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.example.myapp01.databinding.ActivityMainBinding
import android.util.Log
import android.widget.Button
import android.widget.EditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    // Define a list of currencies to support
    private val currencies = listOf("CAD", "USD", "TWD", "JPY")


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get references to UI elements
        val currencyEditText = binding.editText
        val convertButton = binding.convertButton
        val resultTextView = binding.resultTextView

        convertButton.setOnClickListener {
            // Hide the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currencyEditText.windowToken, 0)

            // Get currency amount from the EditText
            val currencyAmount = currencyEditText.text.toString().toDoubleOrNull()

            // If the input is not a valid number, show an error message and return
            if (currencyAmount == null) {
                Snackbar.make(binding.root, "Please enter a valid number", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get the selected currencies
            val fromCurrency = currencies[binding.fromSpinner.selectedItemPosition]
            val toCurrency = currencies[binding.toSpinner.selectedItemPosition]

            // Convert the currency using the exchange rate API
            fetchExchangeRate(fromCurrency, toCurrency, currencyAmount, onSuccess = { convertedAmount ->
                // Show the converted amount in the resultTextView
                resultTextView.text = convertedAmount.toString()
            }, onError = { error ->
                // Show an error message in a Snackbar
                Snackbar.make(binding.root, "Error: $error", Snackbar.LENGTH_SHORT).show()
            })
        }

        // Populate the spinners with currencies
        ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.fromSpinner.adapter = adapter
            binding.toSpinner.adapter = adapter
        }
    }



    private fun fetchExchangeRate(fromCurrency: String, toCurrency: String, amount: Double, onSuccess: (Double) -> Unit, onError: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "https://api.exchangerate-api.com/v4/latest/$fromCurrency"
            try {
                val apiResult = URL(url).readText()
                val jsonObject = JSONObject(apiResult)
                val rates = jsonObject.getJSONObject("rates")
                val fromRate = rates.getDouble(fromCurrency)
                val toRate = rates.getDouble(toCurrency)
                val convertedAmount = amount / fromRate * toRate
                Log.d("MainActivity", "$amount $fromCurrency = $convertedAmount $toCurrency")
                withContext(Dispatchers.Main) {
                    onSuccess(convertedAmount)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", e.toString())
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }


}
