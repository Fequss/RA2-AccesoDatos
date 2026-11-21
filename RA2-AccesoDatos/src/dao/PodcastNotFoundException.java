package dao;

public class PodcastNotFoundException extends Exception{

	private static final long serialVersionUID = 1L;
	
	public PodcastNotFoundException(int idPodcast) {
		super("No existe ningún podcast con id " +idPodcast);
	}
}
