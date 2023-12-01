package net.guides.springboot2.springboot2jpacrudexample.controller;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.InputStreamReader;
import javax.crypto.Mac;
import com.google.zxing.qrcode.QRCodeWriter;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import java.util.EnumMap;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.net.URL;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;

import jakarta.validation.Valid;
import net.guides.springboot2.springboot2jpacrudexample.exception.ResourceNotFoundException;
import net.guides.springboot2.springboot2jpacrudexample.model.Employee;
import net.guides.springboot2.springboot2jpacrudexample.repository.EmployeeRepository;

@CrossOrigin(origins = "http://192.168.0.215:4200")
@RestController
public class EmployeeController {
	@Autowired
	private EmployeeRepository employeeRepository;

	private static final String BASE64_PREFIX = "data:image/png;base64,";
	private static final int QR_CODE_WHITESPACE_MARGIN = 2;
	private static final String DEFAULT_IMAGE_FORMAT = "png";
	private static final String UTF_8_CHARSET = "UTF-8";
	
	@GetMapping("/employees")
	public List<Employee> getAllEmployees() {
		return employeeRepository.findAll();
	}

	@GetMapping("/employees/{id}")
	public ResponseEntity<Employee> getEmployeeById(@PathVariable(value = "id") Long employeeId)
			throws ResourceNotFoundException {
		Employee employee = employeeRepository.findById(employeeId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));
		return ResponseEntity.ok().body(employee);
	}

	@PostMapping("/employees")
	public Employee createEmployee(@Valid @RequestBody Employee employee) {
		return employeeRepository.save(employee);
	}

	@PutMapping("/employees/{id}")
	public ResponseEntity<Employee> updateEmployee(@PathVariable(value = "id") Long employeeId,
			@Valid @RequestBody Employee employeeDetails) throws ResourceNotFoundException {
		Employee employee = employeeRepository.findById(employeeId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));

		employee.setEmailId(employeeDetails.getEmailId());
		employee.setLastName(employeeDetails.getLastName());
		employee.setFirstName(employeeDetails.getFirstName());
		final Employee updatedEmployee = employeeRepository.save(employee);
		return ResponseEntity.ok(updatedEmployee);
	}

	@DeleteMapping("/employees/{id}")
	public Map<String, Boolean> deleteEmployee(@PathVariable(value = "id") Long employeeId)
			throws ResourceNotFoundException {
		Employee employee = employeeRepository.findById(employeeId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found for this id :: " + employeeId));

		employeeRepository.delete(employee);
		Map<String, Boolean> response = new HashMap<>();
		response.put("deleted", Boolean.TRUE);
		return response;
	}
	
	@GetMapping(value = "/employees/Pay/{vo}")
	public String showPage(@PathVariable(value ="vo") String merchantRef) throws IOException {
		
		String merchantRefNo = merchantRef;
		String amount = "10";//amount to send
		String requestType = "UPIQR";
		String currency = "356";// Code numeric of various nations
		String merchantID = "T_99926";
		String addlParam1 = "dfg";
		String addlParam2 = "rtyrt";
		String secureTokenHash = "Y";
		String invoiceDate = "20230317";
		String hashtext = amount + currency + merchantID + merchantRefNo;
		String key1 = hmacDigest(hashtext, "abc");
		
		String hashtext1 = addlParam1 + addlParam2 + amount + currency + invoiceDate + merchantID + merchantRefNo
				+ requestType + secureTokenHash;

		String digest = hmacDigest(hashtext1, key1);
		
		String url = "https://qa.phicommerce.com/pg/api/generateQR";
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		String urlParams = "addlParam1=" + addlParam1 + "&addlParam2=" + addlParam2 + "&amount=" + amount + "&currency="
				+ currency + "&invoiceDate=" + invoiceDate + "&merchantID=" + merchantID + "&merchantRefNo="
				+ merchantRefNo + "&requestType=" + requestType + "&secureTokenHash=" + secureTokenHash + "&secureHash="
				+ digest.trim() + "";

		con.setDoOutput(true);
		con.setDoInput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParams);
		wr.flush();
		wr.close();
		System.out.println("Sending POST request to URL : " + urlParams);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//model.addAttribute("response", response.toString());

		JSONObject json1 = null;
		String upiQR = "";
		JSONObject json = new JSONObject(response.toString());
		String qr = null;
		if (json.get("respBody") != null && !json.get("respBody").toString().equals("null")
				&& !json.get("respBody").toString().equals("")) {
			json1 = new JSONObject(json.get("respBody").toString());
			upiQR = json1.get("upiQR").toString();

			 qr = toBase64QrCode(upiQR, 200, 200);
			//model.addAttribute("qr", qr);
			System.out.println(qr);
		}
		
		return qr;
		
	}
	
	public static String hmacDigest(String msg, String keyString) {
		String digest = null;
		try {
			SecretKeySpec key = new SecretKeySpec((keyString).getBytes("UTF-8"), "HmacSHA256");
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(key);
			byte[] bytes = mac.doFinal(msg.getBytes("ASCII"));
			StringBuffer hash = new StringBuffer();
			for (int i = 0; i < bytes.length; i++) {
				String hex = Integer.toHexString(0xFF & bytes[i]);
				if (hex.length() == 1) {
					hash.append('0');
				}
				hash.append(hex);
			}

			digest = hash.toString();
		} catch (UnsupportedEncodingException e) {
		} catch (InvalidKeyException e) {
		} catch (NoSuchAlgorithmException e) {
		}
		return digest;
	}
	
	public static String toBase64QrCode(final String input, final int width, final int height) {
		try {
			final BufferedImage bufferedImage = toQrCode(input, width, height);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, DEFAULT_IMAGE_FORMAT, outputStream);
			return BASE64_PREFIX + new String(java.util.Base64.getEncoder().encode(outputStream.toByteArray()));
		} catch (Exception e) {

			throw new RuntimeException(e);
		}
	}
	
	public static BufferedImage toQrCode(final String input, final int width, final int height) {
		final QRCodeWriter barcodeWriter = new QRCodeWriter();
		try {
			final Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
			hints.put(EncodeHintType.CHARACTER_SET, UTF_8_CHARSET);
			hints.put(EncodeHintType.MARGIN, QR_CODE_WHITESPACE_MARGIN);
			final BitMatrix bitMatrix = barcodeWriter.encode(input, BarcodeFormat.QR_CODE, width, height, hints);
			return MatrixToImageWriter.toBufferedImage(bitMatrix);
		} catch (Exception e) {
			
			throw new RuntimeException(e);
		}
	}
}
