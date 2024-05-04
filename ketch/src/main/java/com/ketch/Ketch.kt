package com.ketch

import android.content.Context
import com.ketch.internal.download.DownloadManager
import com.ketch.internal.download.DownloadRequest
import com.ketch.internal.download.DownloadRequestListener
import com.ketch.internal.utils.ExceptionConst
import com.ketch.internal.utils.FileUtil
import kotlinx.coroutines.flow.StateFlow

class Ketch private constructor(
    private val context: Context,
    private val downloadConfig: DownloadConfig,
) {

    companion object {
        fun init(
            context: Context,
            downloadConfig: DownloadConfig = DownloadConfig(),
        ): Ketch {

            return Ketch(
                context = context.applicationContext,
                downloadConfig = downloadConfig,
            )
        }
    }

    private val downloadManager = DownloadManager(
        context = context,
    )

    @Synchronized
    fun download(
        url: String,
        path: String = FileUtil.getDefaultDownloadPath(),
        fileName: String = FileUtil.getFileNameFromUrl(url),
        tag: String? = null,
        headers: HashMap<String, String> = hashMapOf(),
        onQueue: () -> Unit = {},
        onStart: (length: Long) -> Unit = {},
        onProgress: (progress: Int, speedInBytePerMs: Float) -> Unit = { _, _ -> },
        onSuccess: () -> Unit = {},
        onFailure: (error: String) -> Unit = {},
        onCancel: () -> Unit = {}
    ): Request {

        if (url.isEmpty() || path.isEmpty() || fileName.isEmpty()) {
            throw RuntimeException(ExceptionConst.EXCEPTION_PARAM_MISSING)
        }

        val downloadRequest = DownloadRequest(
            url = url,
            path = path,
            fileName = fileName,
            tag = tag,
            headers = headers,
        )
        return download(
            downloadRequest = downloadRequest,
            onQueue = onQueue,
            onStart = onStart,
            onProgress = onProgress,
            onSuccess = onSuccess,
            onFailure = onFailure,
            onCancel = onCancel
        )
    }

    private fun download(
        downloadRequest: DownloadRequest,
        onQueue: () -> Unit = {},
        onStart: (length: Long) -> Unit = {},
        onProgress: (progress: Int, speedInBytePerMs: Float) -> Unit = { _, _ -> },
        onSuccess: () -> Unit = {},
        onFailure: (error: String) -> Unit = {},
        onCancel: () -> Unit = {}
    ): Request {
        val listener = object : DownloadRequestListener {
            override fun onQueue() {
                onQueue.invoke()
            }

            override fun onStart(length: Long) {
                onStart.invoke(length)
            }

            override fun onProgress(progress: Int, speedInBytePerMs: Float) {
                onProgress.invoke(progress, speedInBytePerMs)
            }

            override fun onSuccess() {
                onSuccess.invoke()
            }

            override fun onFailure(error: String) {
                onFailure.invoke(error)
            }

            override fun onCancel() {
                onCancel.invoke()
            }
        }
        downloadRequest.listener = listener
        downloadRequest.downloadConfig = downloadConfig
        downloadRequest.timeQueued = System.currentTimeMillis()
        downloadManager.download(downloadRequest)
        return Request(
            id = downloadRequest.id,
            url = downloadRequest.url,
            path = downloadRequest.path,
            fileName = downloadRequest.fileName,
            tag = downloadRequest.tag
        )
    }

    @Synchronized
    fun cancel(id: Int) {
        downloadManager.cancel(id)
    }

    @Synchronized
    fun cancel(tag: String) {
        downloadManager.cancel(tag)
    }

    @Synchronized
    fun cancelAll() {
        downloadManager.cancelAll()
    }

    @Synchronized
    fun observeDownloads(): StateFlow<List<DownloadModel>> {
        return downloadManager.downloadItems //Observe download items requested by this class instance only.
    }

    @Synchronized
    fun stopObserving() {
        downloadManager.stopObserving()
    }

}