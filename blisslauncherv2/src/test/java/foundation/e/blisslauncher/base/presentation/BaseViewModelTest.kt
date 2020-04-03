package foundation.e.blisslauncher.base.presentation

import foundation.e.blisslauncher.common.subscribeToState
import io.mockk.mockk
import org.junit.Before
import org.junit.Test


fun main() {
    print("Amit")
}
class BaseViewModelTest {

    private lateinit var viewModel: BaseViewModel<DummyEvent, DummyState>

    @Before
    fun setUp() {
        viewModel = mockk()
        viewModel.states().subscribeToState {it.print()}
    }

    @Test
    fun testEvent() {
        var event = DummyEvent.Event1
        if(event == DummyEvent.Event1) {
            viewModel.process(intentForEvent1())
        }
    }

    private fun processEvent1(onSuccess: (String) -> Unit) {
        onSuccess("Title changed from Event1")
    }

    private fun intentForEvent1(): BaseIntent<DummyState> {
        return intent {
            processEvent1 {
                viewModel.process(intent {
                    copy(title = it)
                })
            }
            copy()
        }
    }
}

data class DummyState(val id: Int, val title: String, val description: String): BaseViewState {
    fun print() {
        println("Id: $id, Title: $title, Desc: $description")
    }
}

sealed class DummyEvent: BaseViewEvent {
    object Event1: DummyEvent()
    object Event2: DummyEvent()
}