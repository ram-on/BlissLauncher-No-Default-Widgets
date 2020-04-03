package foundation.e.blisslauncher.common.executors

import java.util.concurrent.Executor

data class AppExecutors(val io: Executor, val computation: Executor, val main: Executor)
