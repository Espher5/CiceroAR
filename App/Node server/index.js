const express = require('express');
const app = express(); 
const cors = require('cors');
app.use(cors());

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
  

app.get('/', function(req,res){ 
    res.send('Hello'); 
}); 
  

app.post('/upload', upload.single('image'), (req, res) => {  
    var fs = require('fs');
    var request = require('request');

    const options = {
        method: 'POST',
        url: 'http://localhost:8000/upload/',
        port: 8000,
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        formData: {
            'image': fs.createReadStream('./uploads/' + req.file.filename)
        }
    };

    request(options, function(error, res, body) {
        if(error) {
            throw error;
        }
        console.log(body);
    });
}); 
    

app.listen(8080,function(error) { 
    if(error) {
        throw error;
    }
    console.log("Server created successfully on PORT 8080"); 
});