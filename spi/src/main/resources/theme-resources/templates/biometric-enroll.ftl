<#import "template.ftl" as layout>

<@layout.registrationLayout displayMessage=!messagesPerField.existsError('enrollImage') displayInfo=false; section>

    <#if section = "header">
        Registro Biom&eacute;trico - Reconocimiento Facial
    <#elseif section = "form">

        <div id="enrollment-container" style="text-align: center;">
            <p style="margin-bottom: 8px; color: #6c757d;">
                Hola <strong>${username!""}</strong>, necesitas registrar tu perfil biom&eacute;trico facial.
            </p>
            <p style="margin-bottom: 16px; color: #6c757d; font-size: 13px;">
                Captura al menos <strong>3</strong> im&aacute;genes desde diferentes &aacute;ngulos para un mejor reconocimiento.
                M&aacute;ximo: <strong>${maxImages!5}</strong> im&aacute;genes.
            </p>

            <div style="position: relative; display: inline-block; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.15);">
                <video id="enroll-video" width="480" height="360" autoplay playsinline
                       style="display: block; background: #1a1a2e;"></video>
                <canvas id="enroll-canvas" width="480" height="360" style="display: none;"></canvas>
                <div id="face-guide" style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
                     width: 200px; height: 260px; border: 3px dashed rgba(255,255,255,0.6); border-radius: 50%;
                     pointer-events: none;"></div>
            </div>

            <div id="enroll-status" style="margin-top: 12px; font-size: 14px; color: #495057;">
                Iniciando c&aacute;mara...
            </div>

            <div id="capture-counter" style="margin-top: 8px; font-weight: 600; font-size: 18px; color: #0d6efd;">
                0 / ${maxImages!5} capturas
            </div>

            <div id="thumbnails" style="display: flex; gap: 8px; justify-content: center; flex-wrap: wrap; margin-top: 12px; min-height: 60px;">
            </div>

            <form id="enrollment-form" action="${url.loginAction}" method="post" style="margin-top: 16px;">
                <div id="hidden-fields"></div>

                <button id="capture-btn" type="button" disabled
                        class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!}"
                        style="margin-right: 8px;"
                        onclick="captureImage()">
                    Capturar Imagen
                </button>

                <button id="submit-btn" type="button" disabled
                        class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}"
                        style="margin-top: 12px;"
                        onclick="submitEnrollment()">
                    Completar Registro Biom&eacute;trico
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
                var video = document.getElementById('enroll-video');
                var canvas = document.getElementById('enroll-canvas');
                var captureBtn = document.getElementById('capture-btn');
                var submitBtn = document.getElementById('submit-btn');
                var statusEl = document.getElementById('enroll-status');
                var counterEl = document.getElementById('capture-counter');
                var thumbnailsEl = document.getElementById('thumbnails');
                var hiddenFields = document.getElementById('hidden-fields');
                var faceGuide = document.getElementById('face-guide');

                var capturedImages = [];
                var maxImages = ${maxImages!5};
                var requiredImages = ${requiredImages!3};

                navigator.mediaDevices.getUserMedia({
                    video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' }
                }).then(function(stream) {
                    video.srcObject = stream;
                    captureBtn.disabled = false;
                    statusEl.textContent = 'C\u00e1mara lista. Captura im\u00e1genes desde diferentes \u00e1ngulos.';
                    faceGuide.style.borderColor = 'rgba(0, 200, 83, 0.7)';
                }).catch(function(err) {
                    statusEl.textContent = 'Error al acceder a la c\u00e1mara: ' + err.message;
                    statusEl.style.color = '#dc3545';
                });

                var angles = [
                    'Mira de frente a la c\u00e1mara',
                    'Gira ligeramente a la izquierda',
                    'Gira ligeramente a la derecha',
                    'Inclina la cabeza ligeramente hacia arriba',
                    'Vuelve a mirar de frente'
                ];

                window.captureImage = function() {
                    if (capturedImages.length >= maxImages) return;

                    var ctx = canvas.getContext('2d');
                    canvas.width = video.videoWidth;
                    canvas.height = video.videoHeight;
                    ctx.drawImage(video, 0, 0);

                    var dataUrl = canvas.toDataURL('image/jpeg', 0.9);
                    capturedImages.push(dataUrl);

                    var thumb = document.createElement('img');
                    thumb.src = dataUrl;
                    thumb.style.cssText = 'width:60px;height:60px;object-fit:cover;border-radius:8px;border:2px solid #0d6efd;';
                    thumbnailsEl.appendChild(thumb);

                    var count = capturedImages.length;
                    counterEl.textContent = count + ' / ' + maxImages + ' capturas';

                    if (count >= requiredImages) {
                        submitBtn.disabled = false;
                        submitBtn.style.opacity = '1';
                    }

                    if (count >= maxImages) {
                        captureBtn.disabled = true;
                        statusEl.textContent = 'M\u00e1ximo de capturas alcanzado. Presiona "Completar Registro".';
                    } else if (count < angles.length) {
                        statusEl.textContent = angles[count];
                        faceGuide.style.borderColor = 'rgba(255, 193, 7, 0.8)';
                        setTimeout(function() { faceGuide.style.borderColor = 'rgba(0, 200, 83, 0.7)'; }, 500);
                    }
                };

                window.submitEnrollment = function() {
                    if (capturedImages.length < requiredImages) {
                        statusEl.textContent = 'Necesitas al menos ' + requiredImages + ' capturas.';
                        statusEl.style.color = '#dc3545';
                        return;
                    }

                    hiddenFields.innerHTML = '';
                    for (var i = 0; i < capturedImages.length; i++) {
                        var input = document.createElement('input');
                        input.type = 'hidden';
                        input.name = 'enrollImage_' + i;
                        input.value = capturedImages[i];
                        hiddenFields.appendChild(input);
                    }

                    captureBtn.disabled = true;
                    submitBtn.disabled = true;
                    submitBtn.textContent = 'Registrando...';
                    statusEl.textContent = 'Enviando im\u00e1genes biom\u00e9tricas al servidor...';
                    faceGuide.style.borderColor = 'rgba(255, 193, 7, 0.8)';

                    if (video.srcObject) {
                        video.srcObject.getTracks().forEach(function(t) { t.stop(); });
                    }

                    document.getElementById('enrollment-form').submit();
                };
            })();
        </script>

    </#if>

</@layout.registrationLayout>
