const express = require('express');
const app = express(); 
const cors = require('cors');
app.use(cors());
app.use('/paintings', express.static(__dirname + '/paintings'));


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
    request(options, (err, APIresponse, body) => {
        if(err) throw err;
        var json = JSON.parse(body);
        var title = json.title;

        if(title != null) {
            const MongoClient  = require('mongodb').MongoClient;
            const dbUrl = 'mongodb://localhost:27017/';
            MongoClient.connect(dbUrl, { useUnifiedTopology: true }, (err, db) => {
                var dbo = db.db('PaintingsInfo')
                dbo.collection('Paintings').findOne({ title: title}, (err, result) => {
                    if(err) throw err;
                    db.close;
                    res.json(result);
                });
            });
        }

        fs.unlink(filename, (err) => {
            if(err) throw err;
        });
    });  
}); 
    

app.listen(8080, function(err) { 
    if(err) throw err;
    console.log("Server created successfully on PORT 8080"); 
});