//
//  ContentView.swift
//  LiveChatDemo
//

import SwiftUI
import WebKit
import CommonCrypto

struct WebView: UIViewRepresentable {
    let url: URL
    let enableConsole: Bool       // 是否开启 WebView 控制台日志

    func makeCoordinator() -> Coordinator {
        Coordinator(enableConsole: enableConsole)
    }
  
    func makeUIView(context: Context) -> WKWebView {
            let config = WKWebViewConfiguration()
            
            // 捕获 console.log
            if enableConsole {
                let source = """
                console.log = (function(origLog) {
                    return function(...args) {
                        window.webkit.messageHandlers.console.postMessage(args.join(' '));
                        origLog.apply(console, args);
                    };
                })(console.log);
                """
                let script = WKUserScript(source: source, injectionTime: .atDocumentStart, forMainFrameOnly: false)
                config.userContentController.addUserScript(script)
                config.userContentController.add(context.coordinator, name: "console")
            }

            let webView = WKWebView(frame: .zero, configuration: config)
            webView.load(URLRequest(url: url))
            return webView
        }
  
    func updateUIView(_ uiView: WKWebView, context: Context) {
        uiView.load(URLRequest(url: url))
    }
    
    class Coordinator: NSObject, WKScriptMessageHandler {
            let enableConsole: Bool

            init(enableConsole: Bool) {
                self.enableConsole = enableConsole
            }

            func userContentController(_ userContentController: WKUserContentController,
                                       didReceive message: WKScriptMessage) {
                if enableConsole, message.name == "console" {
                    print("🌐 JS:", message.body)
                }
            }
        }
}

struct ContentView: View {
    @State private var url: URL = URL(string: "about:blank")!
    // 是否开启 WebView 控制台日志
    let enableConsoleLog = true
    var body: some View {
        ZStack {
            WebView(url: url, enableConsole: enableConsoleLog)
                .edgesIgnoringSafeArea(.all)
        }
        .onAppear {
            do {
                let realUrl = try buildUrl()
                print("最终 URL:", realUrl.absoluteString)
                self.url = realUrl
            } catch {
                print("构建 URL 错误:", error)
            }
        }
    }
}

func aesEncrypt(plainText: String) throws -> String {
    // 联系客服获取的密钥和向量
    let encryptionKey = "您的加密密钥"
    let encryptionIv  = "您的IV向量"

    guard encryptionKey.count == 16 else { throw NSError(domain: "Key must be 16 bytes", code: -1) }
    guard encryptionIv.count  == 16 else { throw NSError(domain: "IV must be 16 bytes", code: -1) }

    let keyData = encryptionKey.data(using: .utf8)!
    let ivData  = encryptionIv.data(using: .utf8)!
    let dataToEncrypt = plainText.data(using: .utf8)!

    let encryptedData = try aesCBCEncrypt(data: dataToEncrypt, keyData: keyData, ivData: ivData)
    return encryptedData.base64EncodedString()
}

private func aesCBCEncrypt(data: Data, keyData: Data, ivData: Data) throws -> Data {
    let keyLength = kCCKeySizeAES128

    var outLength = Int(0)
    var outBytes = [UInt8](repeating: 0, count: data.count + kCCBlockSizeAES128)
    var status: CCCryptorStatus = CCCryptorStatus(kCCSuccess)

    data.withUnsafeBytes { dataBytes in
        keyData.withUnsafeBytes { keyBytes in
            ivData.withUnsafeBytes { ivBytes in
                status = CCCrypt(
                    CCOperation(kCCEncrypt),
                    CCAlgorithm(kCCAlgorithmAES),
                    CCOptions(kCCOptionPKCS7Padding), // PKCS5 = PKCS7
                    keyBytes.baseAddress,
                    keyLength,
                    ivBytes.baseAddress,
                    dataBytes.baseAddress,
                    data.count,
                    &outBytes,
                    outBytes.count,
                    &outLength
                )
            }
        }
    }

    guard status == kCCSuccess else {
        throw NSError(domain: "AES Encryption Error", code: Int(status))
    }

    return Data(bytes: outBytes, count: outLength)
}

func buildUrl() throws -> URL {
    let pluginId = "Your Plugin ID"

    let plainJson = """
    {"name":"IOS-test1","email":"ios.test1@example.org","phone":"13800138001","visitorId":"34e535ea-67cd-49d8-a643-3ea35cd40278"}
    """

    let encrypted = try aesEncrypt(plainText: plainJson)

    let encoded = encrypted.addingPercentEncoding(
        withAllowedCharacters: CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~")
    )!

    let urlString = "https://chat-plugin.hiifans.com/native.html?pluginId=\(pluginId)&enc=\(encoded)"
    print("loadUrl: \(urlString)")

    return URL(string: urlString)!
}
#Preview {
    ContentView()
}
