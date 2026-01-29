# Security Angel 🛡️

**Security Angel** is a comprehensive mobile security ecosystem designed to protect families and individuals from modern digital threats. It goes beyond simple scanning by integrating an **actionable AI assistant** that understands context, analyzes visual data, and proactively guards user privacy.

---

## 🚀 Key Features & AI Capabilities

### 🧠 1. AI Security Assistant (The Brain)
At the core of the app lies an intelligent security agent, Unlike standard chatbots, this assistant is fully integrated into the user's security context:

* **👁️ Computer Vision & Phishing Detection:**
  The AI utilizes advanced vision capabilities to "see" what the user sees. Users can upload screenshots of suspicious SMS messages, emails, or invoices. The model analyzes the image pixel-by-pixel, extracts text, detects visual fraud patterns (like fake logos or urgent phrasing), and provides an immediate verdict: **SAFE**, **SUSPICIOUS**, or **DANGEROUS**.
* **Context-Aware Intelligence:**
  The assistant doesn't operate in a vacuum. Before answering, the app injects a dynamic "System Context" into the AI (RAG - Retrieval-Augmented Generation). This includes the user's current password leak count, family risk status, and recent scan results. This allows the AI to give personalized advice, such as: *"I see 3 of your passwords were leaked recently, let's change them now."*
* **🤖 Actionable AI (Deep Linking):**
  The AI is capable of performing actions, not just talking. It identifies user intent (e.g., "Check my family status" or "Generate a password") and automatically navigates the user to the relevant screen using internal deep links.

### 🔍 2. Smart Sandbox URL Scanner
A robust defense mechanism for analyzing links before clicking.
* **Aggregated Analysis:** Powered by the **VirusTotal API**, the scanner queries the URL against **over 70 antivirus engines** simultaneously.
* **Smart Polling & Automation:** The system automatically handles new/unknown URLs by submitting them for deep analysis on remote servers and polling for results in real-time, ensuring the user never receives partial data.
* **Threat Classification:** Distinguishes between Malware, Phishing, and Adult Content to provide accurate warnings.

### 👨‍👩‍👧‍👦 3. Real-Time Family Safety Net
A centralized dashboard for family heads and parents.
* **Risk Monitoring:** The app calculates a "Risk Score" for each family member in real-time. If a child's password is leaked or they visit a malicious site, the Admin receives an immediate indication.
* **Secure Invitations:** Manage family groups using encrypted, one-time invitation codes.

### 🔐 4. Intelligent Password Vault
* **Leak Detection:** The vault proactively cross-references stored credentials against global breach databases. Users are alerted instantly if their saved passwords appear in known data dumps.
* **Zero-Knowledge Architecture:** Designed to keep credentials secure and accessible only to the user.

---

## 🛠️ Installation & Setup

1.  **Clone the repository.**
2.  **Firebase Setup:** Add your `google-services.json` file to the `app/` directory.
3.  **API Configuration:** Add your API keys to `local.properties`:
    ```properties
    GEMINI_API_KEY=your_gemini_key_here
    VIRUSTOTAL_API_KEY=your_virustotal_key_here
    ```
4.  **Build & Run:** Deploy the app on an Android device or emulator.

---
Developed by **Liran Zozulya**.