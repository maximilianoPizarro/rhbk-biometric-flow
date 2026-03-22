<#import "template.ftl" as layout>

<@layout.registrationLayout displayMessage=!messagesPerField.existsError('biometricImage') displayInfo=false; section>

    <#if section = "header">
        Verificaci&oacute;n Biom&eacute;trica
    <#elseif section = "form">

        <div id="biometric-container" style="text-align: center;">
            <p style="margin-bottom: 16px; color: #6c757d;">
                Hola <strong>${username!""}</strong>, por favor mira directamente a la c&aacute;mara para verificar tu identidad.
            </p>

            <div style="position: relative; display: inline-block; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.15);">
                <video id="biometric-video" width="480" height="360" autoplay playsinline
                       style="display: block; background: #1a1a2e;"></video>
                <canvas id="biometric-canvas" width="480" height="360" style="display: none;"></canvas>
                <div id="face-overlay" style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
                     width: 200px; height: 260px; border: 3px dashed rgba(255,255,255,0.6); border-radius: 50%;
                     pointer-events: none;"></div>
            </div>

            <div id="biometric-status" style="margin-top: 12px; font-size: 14px; color: #495057;">
                Iniciando c&aacute;mara...
            </div>

            <form id="biometric-form" action="${url.loginAction}" method="post" style="margin-top: 16px;">
                <input type="hidden" id="biometricImage" name="biometricImage" value="" />

                <button id="capture-btn" type="button" disabled
                        class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                        style="margin-bottom: 8px;"
                        onclick="captureAndSubmit()">
                    Capturar y Verificar
                </button>

                <#if message?has_content && (message.type == 'error' || message.type == 'warning')>
                    <div class="${properties.kcAlertClass!} pf-m-danger" style="margin-top: 12px;">
                        <div class="${properties.kcAlertIconClass!}">
                            <svg aria-hidden="true" fill="currentColor" viewBox="0 0 512 512" width="1em" height="1em">
                                <path d="M256 0C114.6 0 0 114.6 0 256s114.6 256 256 256 256-114.6 256-256S397.4 0 256 0zm0 384c-13.2 0-24-10.8-24-24s10.8-24 24-24 24 10.8 24 24-10.8 24-24 24zm24-112c0 13.2-10.8 24-24 24s-24-10.8-24-24v-128c0-13.2 10.8-24 24-24s24 10.8 24 24v128z"/>
                            </svg>
                        </div>
                        <span class="${properties.kcAlertTitleClass!}">${kcSanitize(message.summary)?no_esc}</span>
                    </div>
                </#if>
            </form>
        </div>

        <script>
            (function() {
                var video = document.getElementById('biometric-video');
                var canvas = document.getElementById('biometric-canvas');
                var captureBtn = document.getElementById('capture-btn');
                var statusEl = document.getElementById('biometric-status');
                var overlay = document.getElementById('face-overlay');

                navigator.mediaDevices.getUserMedia({
                    video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' }
                }).then(function(stream) {
                    video.srcObject = stream;
                    captureBtn.disabled = false;
                    statusEl.textContent = 'C\u00e1mara lista. Centra tu rostro en el \u00f3valo y presiona "Capturar y Verificar".';
                    overlay.style.borderColor = 'rgba(0, 200, 83, 0.7)';
                }).catch(function(err) {
                    statusEl.textContent = 'Error al acceder a la c\u00e1mara: ' + err.message;
                    statusEl.style.color = '#dc3545';
                });

                window.captureAndSubmit = function() {
                    var ctx = canvas.getContext('2d');
                    canvas.width = video.videoWidth;
                    canvas.height = video.videoHeight;
                    ctx.drawImage(video, 0, 0);

                    var dataUrl = canvas.toDataURL('image/jpeg', 0.9);
                    document.getElementById('biometricImage').value = dataUrl;

                    captureBtn.disabled = true;
                    captureBtn.textContent = 'Verificando...';
                    statusEl.textContent = 'Procesando verificaci\u00f3n biom\u00e9trica...';
                    overlay.style.borderColor = 'rgba(255, 193, 7, 0.8)';

                    if (video.srcObject) {
                        video.srcObject.getTracks().forEach(function(t) { t.stop(); });
                    }

                    document.getElementById('biometric-form').submit();
                };
            })();
        </script>

    </#if>

</@layout.registrationLayout>
