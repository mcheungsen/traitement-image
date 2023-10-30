package mcheungsen.backend;

import java.io.*;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
public class ImageController {

  @Autowired
  private ObjectMapper mapper;

  private final ImageDao imageDao;

  @Autowired
  public ImageController(ImageDao imageDao) {
    this.imageDao = imageDao;
  }

  @RequestMapping(value = "/images/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
  public ResponseEntity<?> getImage(@PathVariable("id") long id) {
    byte[] imageData = imageDao.retrieve(id).get().getData();

    if (imageData != null){
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.IMAGE_JPEG);

      return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
    }

    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  @RequestMapping(value = "/images/{id}", method = RequestMethod.DELETE)
  public ResponseEntity<?> deleteImage(@PathVariable("id") long id) {
    if(imageDao.retrieve(id).isPresent()){
      imageDao.delete(imageDao.retrieve(id).get());

      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  @RequestMapping(value = "/images", method = RequestMethod.POST)
  public ResponseEntity<?> addImage(@RequestParam("file") MultipartFile file,
      RedirectAttributes redirectAttributes) {
    if(file.isEmpty()){
      return new ResponseEntity<>("Le fichier est vide.", HttpStatus.BAD_REQUEST);
    }

    if (file.getContentType() != null && file.getContentType().equals("image/jpeg")) {
      try {
        Image img = new Image(file.getOriginalFilename(), file.getBytes());
        imageDao.create(img);

        File f = new File("src/main/resources/images/" + img.getId()+"_" + file.getOriginalFilename());
        OutputStream os = new FileOutputStream(f);
        os.write(file.getBytes());
        os.close();
        return new ResponseEntity<>(f.getPath(), HttpStatus.CREATED);

      } catch (IOException e) {
        return new ResponseEntity<>("Erreur interne du serveur : " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
    return new ResponseEntity<>(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @RequestMapping(value = "/images", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
  @ResponseBody
  public ArrayNode getImageList() {
    ArrayNode nodes = mapper.createArrayNode();
    ObjectMapper mapper = new ObjectMapper();
    for(Image img : imageDao.retrieveAll()){
      ObjectNode obj = mapper.createObjectNode();
      obj.put("id", img.getId());
      obj.put("name",img.getName());
      nodes.add(obj);
    }
    return nodes;
  }

}
