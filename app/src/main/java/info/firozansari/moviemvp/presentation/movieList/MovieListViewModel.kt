package info.firozansari.moviemvp.presentation.movieList

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import info.firozansari.moviemvp.domain.model.Movie
import info.firozansari.moviemvp.domain.model.MovieListType
import info.firozansari.moviemvp.domain.usecase.FavoriteMoviesUseCase
import info.firozansari.moviemvp.domain.usecase.PopularMoviesUseCase
import info.firozansari.moviemvp.domain.usecase.TopRatedMoviesUseCase
import info.firozansari.moviemvp.presentation.ViewModelResult
import info.firozansari.moviemvp.presentation.ViewModelResult.Loading
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantLock

class MovieListViewModel(
        private val favoriteMoviesUseCase: FavoriteMoviesUseCase,
        private val topRatedMoviesUseCase: TopRatedMoviesUseCase,
        private val popularMoviesUseCase: PopularMoviesUseCase
) : ViewModel() {

    private val moviesList = ArrayList<Movie>()
    lateinit var type: MovieListType
        private set
    val onMoviesListReceived = MutableLiveData<ViewModelResult>()

    //region pagination
    var currentPage = FIRST_PAGE
        private set
    var previousSize = moviesList.size
        private set
    private val lock = ReentrantLock()
    //endregion

    fun setup(type: MovieListType) {
        this@MovieListViewModel.type = type
        onMoviesListReceived.postValue(Loading)
        requestMore()
    }

    fun requestMore() {
        var hasMorePages = false
        if (!lock.isLocked) {
            lock.lock()
            GlobalScope.launch {
                try {
                    when (type) {
                        MovieListType.TopRated -> {
                            topRatedMoviesUseCase.execute(currentPage)?.also {
                                hasMorePages = it.hasMorePages
                            }?.movies
                        }
                        MovieListType.Popular -> {
                            popularMoviesUseCase.execute(currentPage)?.also {
                                hasMorePages = it.hasMorePages
                            }?.movies
                        }
                        MovieListType.Favorite -> {
                            favoriteMoviesUseCase.execute()
                        }
                    }?.let { movies ->
                        if (hasMorePages)
                            currentPage++
                        previousSize = moviesList.size
                        moviesList.addAll(movies)
                        if (previousSize == 0) {
                            onMoviesListReceived.postValue(ViewModelResult.Success)
                        } else {
                            onMoviesListReceived.postValue(ViewModelResult.Updated)
                        }
                    }
                } catch (exception: Exception) {
                    Log.e("ERROR", exception.message ?: "--")
                    onMoviesListReceived.postValue(ViewModelResult.Error)
                }
            }
            lock.unlock()
        }
    }

    fun reload() {
        if (::type.isInitialized) {
            currentPage = FIRST_PAGE
            moviesList.clear()
            setup(type)
        }
    }

    fun movies() = moviesList.toList()

    fun newPart() = moviesList.subList(previousSize, moviesList.lastIndex).toList()

    companion object {
        const val FIRST_PAGE = 1
    }

}