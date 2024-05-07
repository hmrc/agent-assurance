import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

//LocalDate.now().format(formatter)

formatter.format(LocalDateTime.now())