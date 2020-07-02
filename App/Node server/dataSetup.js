const { MongoClient } = require('mongodb');
const dbUrl = 'mongodb://localhost:27017/';


MongoClient.connect(dbUrl, { useUnifiedTopology: true }, (err, db) => {
    if(err) throw err;
    console.log('Database created');

    var dbo = db.db('PaintingsInfo');
    dbo.createCollection("Paintings", (err, res) => {
        if(err) throw err;
        console.log('Collection created');  
        db.close();    
    }); 


    var venere = {
        title : 'Venere',
        author : 'Sandro Botticelli',
        paintingDetails : [
            {
                imagePath : null,
                description : 'The main focus of the composition is the goddess of love and beauty, Venus.' + 
                            'Born by the sea spray, she is blown on the island of Cyprus by the winds, Zephyr and Aura.' +
                            'She is met by a young woman, sometimes identified as the Hora of Spring, who holds a cloak covered in flowers' +
                            'and is ready to cover her. A detail often overlooked is the lack of shadows in the scene; ' + 
                            'according to some interpretations, the painting is set in an alternative reality, still very similar to our own.'         
            },
            {
                imagePath : 'http://192.168.43.6:8080/paintings/Venere/Venus.jpg',
                description : 'The goddess is standing on a giant scallop shell, as pure and perfect as a pearl. ' + 
                            'She covers her nakedness with long, blond hair, which has reflections of light from the fact it has been gilded. ' +
                            'The fine modelling and white flesh colour gives her the appearance of a statue, an impression fortified by her stance, ' +
                            'which is very similar to the Venus Pudica, an ancient statue of the greek-roman period.'
            },
            {
                imagePath : 'http://192.168.43.6:8080/paintings/Venere/Shell.jpg',
                description : 'You may wonder why Venus is standing on a shell; the story goes that the God Uranus had a son named Chronus, ' + 
                            'who overthrew his father and threw his genitals into the sea; this caused the water to be fertilised, ' +
                            'and thus the goddess was born.'
            },
            {
                imagePath : 'http://192.168.43.6:8080/paintings/Venere/Zephyrus.jpg',
                description : 'In the top left of the piece we can notice Zephyrus, god of the winds; ' +
                            'he is  holding Aura, personification of a light breeze. The two are highlighting the pale face of the goddess, ' + 
                            'while blowing the shell towards the coast.'
            },
            {
                imagePath : 'http://192.168.43.6:8080/paintings/Venere/Aura.jpg',
                description : 'The Hora herself may be a complementary version of the nymph Chloris. ' +
                            'Are they two versions of the same person then? It might be; the story of this woman is narrated in ' + 
                            '“I Fasti” by latin author Ovidio and the painted in “The Spring”, by Botticelli himself, ' + 
                            'where the woman gets kidnapped by Zephyrus to become a mystical figure. The theory is quite farfetched, ' + 
                            'however there’s a detail in its favour: the roses falling around her and Zephyrus.'
            }
        ]
    }

    dbo.collection('Paintings').insertOne(venere, (err, res) => {
        if(err) throw err;
        console.log('Entry created');
        db.close();
    });
});