package foundation.e.blisslauncher.base.presentation

interface BaseView<in State : BaseViewState> {

    fun render(state: State)
}