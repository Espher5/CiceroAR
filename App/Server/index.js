const express = require('express');
const cors = require('cors');
const multer = require('multer') ;
const app = express(); 
app.use(cors());
   
var storage = multer.diskStorage({ 
    destination: function (req, file, cb) { 
        cb(null, 'uploads') 
    }, 
    filename: function (req, file, cb) { 
      cb(null, file.fieldname + '-' + Date.now()+'.jpg') 
    } 
}); 
          
var upload = multer({  storage: storage });     
  
app.get('/',function(req,res){ 
    res.send('Hello'); 
}) 
    
app.post('/upload', upload.single('image'), (req, res) => { 
    res.send('success');
}); 
    

app.listen(8080,function(error) { 
    if(error) throw error 
        console.log("Server created Successfully on PORT 8080"); 
});