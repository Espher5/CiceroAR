const express = require('express');
const app = express(); 
const cors = require('cors');
app.use(cors());


/**
 * Temporary storage setup
 */
const multer = require('multer') ;
var storage = multer.diskStorage({ 
    destination: function (req, file, cb) { 
        cb(null, 'uploads') 
    }, 
    filename: function (req, file, cb) { 
        cb(null, file.fieldname + '-' + Date.now()+'.jpg') 
    } 
});         
var upload = multer({  storage: storage });     
  

/**
 * Get request handler
 */
app.get('/', function(req,res){ 
    res.send('Hello'); 
}); 
  

/**
 * Post request handler
 * Stores the image in the temporary storage, forwards it to the
 * REST API and deletes the image
 */
app.post('/upload', upload.single('image'), (req, res) => {  
    var fs = require('fs');
    var request = require('request');
    var filename = './uploads/' + req.file.filename;


    /*
    * API request setup
    */
    const options = {
        method: 'POST',
        url: 'http://localhost:8000/upload/',
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        formData: {
            'image': fs.createReadStream(filename)
        }
    };


    /*
    * Forwards the image to the REST API, sends the json response 
    * body back to the Android client and deletes the image
    */
    console.log('Sending request to REST API...');
    request(options, function(err, APIresponse, body) {
        if(err) throw err;
        res.json({
            'artist': 'Sandro Botticelli',
            'title': JSON.parse(body).title,
            'description': 'blablabla'
        });

        fs.unlink(filename, (err) => {
            if(err) throw err;
        });
    });  
}); 
    

app.listen(8080, function(err) { 
    if(err) throw err;
    console.log("Server created successfully on PORT 8080"); 
});